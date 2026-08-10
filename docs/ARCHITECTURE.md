# Architecture guide

This document is the map of the codebase: what every class does, how the screens fit
together, where state lives and which rules must not be broken when refactoring.

Module: `app` — Java, Views/XML (no Compose UI), `minSdk 24`, `compile/targetSdk 36`.

---

## 1. Rules that must not be broken

1. **Never rename or move a class that is declared in `AndroidManifest.xml`.**
   Android stores the user's *default home app* choice and the *granted accessibility
   service* as flattened component names (`package/class`). Renaming such a class makes
   an existing install lose its home-screen role and silently disables the blocking
   engine — while a lock may be active. This is why the manifest components still live
   in the historical `com.genzopia.addiction.Launcher` package.
2. **Never change a SharedPreferences file name, key name or value format.**
   Lock state, allowed apps and earned reward codes of installed versions depend on them.
   All keys are documented in the javadoc of `Launcher/SharedPrefHelper`.
3. **Never do blocking work on the main thread**, and never touch views from a background
   callback without checking `isAdded()` / `getView() != null`. Version 21 shipped several
   ANRs caused exactly by this.
4. **Register every runtime `BroadcastReceiver` with an explicit export flag**
   (`ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`), otherwise Android 14+
   throws `SecurityException`.

---

## 2. Package layout

```
com.genzopia.addiction
├── MyApp                     Application: notification channel, AppRepository bootstrap, FCM topic
│
├── data                      ← data layer (new, framework-free where possible)
│   ├── AppRepository         single source of truth for the installed-app list
│   └── model/AppInfo         immutable app model (label, package, lazy icon)
│
├── ui                        ← reusable UI building blocks (new)
│   └── common/AppListAdapter one RecyclerView adapter for every app list
│
└── Launcher                  ← all manifest-declared components (see rule 1) + legacy helpers
    ├── permission/           onboarding wizard
    ├── launcher/home         MainContainerActivity, HomeFragment, MainFragment, ...
    ├── enforcement           NotificationBarDetectorService, OverlayService, PopupActivity, MyTileService
    ├── challenge             ChallengeDialog, DSAChallengeDialog, InfoDsaDialog, ChallengeReward, Rewards_list
    ├── monetization          BillingActivity, ReviewActivity, ReviewDialog, AppUpdateChecker
    └── infrastructure        SharedPrefHelper, NotificationHelper, AppFcmService, DataPayloadHandler
```

The sub-groups inside `Launcher` are logical, not physical: they cannot be moved into real
sub-packages without renaming manifest components (rule 1). New code should go into `data/`,
`ui/` or a new non-manifest package.

---

## 3. Screen flow

```
MyApp (Application)
 └─ NotificationHelper.createChannel(), AppRepository.registerPackageReceiver(), FCM topic

Launcher.permission.MainActivity        ← LAUNCHER icon: 6-page onboarding wizard
 └─ notifications → overlay → accessibility → terms → theme → default launcher

Launcher.MainContainerActivity          ← HOME (unlocked). ViewPager2:
 ├─ page 0  HomeFragment                minimal home: dialer + one assignable shortcut
 └─ page 1  MainFragment                app drawer, app selection, lock-time picker
                     │  executeMainLogic(): startTime + initialDuration + timeActive=true
                     ▼
Launcher.MainContainerActivity2         ← HOME while locked. ViewPager2:
 ├─ page 0  HomeFragment2               countdown, LeetCode sync
 └─ page 1  SelectedAppsFragment        drawer restricted to the allowed apps

NotificationBarDetectorService (AccessibilityService, always running)
 └─ violation → OverlayService (full-screen overlay) + PopupActivity (blocking dialog)
```

---

## 4. The app list (the part that used to be duplicated)

Before: `AppListViewModel`, `SelectedAppsFragment` and `PopupSelectApp` each queried
`PackageManager` separately, and three adapters (`AppAdapter`, `AppListAdapter`,
`SelectedAppsAdapter`) with three row layouts rendered the same row. The list was also
loaded exactly once per process, so a newly installed app never appeared — the launcher
process lives for days.

Now:

| Piece | Responsibility |
|---|---|
| `data.AppRepository` | queries launchable apps on a daemon executor, caches them, publishes `LiveData<List<AppInfo>>` **only when the content changed**, caches labels, exposes `getAllPackages()`/`toAppInfos()` |
| `data.model.AppInfo` | `label`, `packageName`, lazily resolved `icon` |
| `Launcher.AppListViewModel` | thin `AndroidViewModel` bridge (kept for existing call sites) |
| `ui.common.AppListAdapter` | `ListAdapter` + `DiffUtil` + `Filterable`, configured through `AppListAdapter.Config` (icon, pin indicator, selection mode, launch on click vs long press), and it computes the A–Z `FastScrollView.Section` list |
| `res/layout/item_app_row.xml` | the single row layout (icon and pin are toggled by the adapter) |

**How a newly installed app shows up:** `MyApp` registers one receiver for
`PACKAGE_ADDED/REMOVED/REPLACED/CHANGED` (+ suspend/unsuspend). Every event drops the
package from the label cache and schedules a debounced (500 ms) refresh, so a Play Store
batch update triggers one reload instead of dozens. `MainContainerActivity.onStart()` and
`MainContainerActivity2.onStart()` also call `refresh()` as a cheap safety net.

Consumers: `MainFragment` (selection + pinning + search + fast scroll),
`SelectedAppsFragment` (restricted drawer, DSA reward window), `PopupSelectApp`
(home-screen shortcut picker).

---

## 5. State: `Launcher/SharedPrefHelper`

Four preference files, kept for backwards compatibility:

| File | Contents |
|---|---|
| `AddictionPrefs` | lock state (`timeActive`, `timeLimit`, `startTime`, `initialDuration`), allowed apps (`selectedApp`, `selectedApps`), `pinned_apps`, theming (`DarkMode`, `GrayMode`, `modeNight`, `FollowSystemTheme`), `terms_accepted`, `click_to_open`, review throttle, LeetCode/DSA challenge keys |
| `MyPrefs` | `Challenge_status`, `cheatchallengevalue` |
| `MySharedPref` | `challenge_code` (JSON list of earned reward codes) |
| `MyAppPrefs` | `fcm_token`, `notif_permission_granted`, home-screen `shortcut` |

`getTimeActivateStatus()` is self-expiring: when `now - startTime >= initialDuration * 1000`
it writes `timeActive = false`. That single method ends lock mode everywhere.

Every setter now takes a **fresh** `SharedPreferences.Editor`; one cached editor used to be
shared by all setters, which could publish or drop unrelated pending writes.
`writeData()` writes the allowed apps and the lock flag in one atomic commit.

---

## 6. Enforcement engine

`Launcher/NotificationBarDetectorService` (config: `res/xml/accessibility_service_config.xml`):

- does nothing unless `getTimeActivateStatus()` is true;
- on `TYPE_WINDOW_STATE_CHANGED` compares the foreground package with the allowed list and
  the never-block list (`SharedPrefHelper.appWithNoWarning()`: own package, system UI,
  launchers, Play Store/Services and payment apps);
- never blocks the system credential screen (keyguard/pin/pattern/biometric class names, and
  the window captured while `AuthenticationManager` is in state `going`);
- anti-tamper: scans the node tree on a dedicated `HandlerThread` (400 ms, depth-capped,
  child nodes recycled) for the app's own name plus *uninstall / force stop / clear data /
  accessibility*, and blocks that screen;
- blocking is throttled to one popup per 2 s — spamming the window manager was one of the
  "Input dispatching timed out (No focused window)" ANRs;
- posts the countdown notification and pauses the ticker while the screen is off; the
  screen on/off receiver runs on the worker thread, not the main thread.

---

## 7. Monetization / platform integration

- `BillingActivity`, and the unlock buttons in `PopupActivity` / `SelectedAppsFragment`:
  one consumable product `unlock_discipline_lock_v2`; consuming it resets `timeLimit`,
  `timeActive` and the DSA challenge. Purchases are verified locally only.
- `AppFcmService` → `DataPayloadHandler` routes on the `type` data key:
  `show_notification`, `force_update` (app-private broadcast, `setPackage()`), `config_refresh`.
- `NotificationHelper`: channels `fcm_default_channel` (high) and `timer_channel` (low).
- `AppUpdateChecker`: IMMEDIATE in-app update, guarded against a second launch and against a
  finishing activity; the activity removes the observer in `onDestroy` to avoid leaking it
  for the whole process lifetime.
- `MyTileService`: Quick-Settings tile; uses `startActivityAndCollapse(PendingIntent)` on
  Android 14+ and never blocks in `onClick`.

---

## 8. Tests

`app/src/test/java/com/genzopia/addiction/` — JUnit platform + jqwik property tests
(`unitTests.all { useJUnitPlatform() }` in `app/build.gradle.kts`, `isReturnDefaultValues = true`,
so tests must stay pure Java and must not rely on real `TextUtils`/`Log` behaviour):

| Test | Covers |
|---|---|
| `AppListLogicPropertyTest` | search filtering (subset, matches only, pinned first), A–Z section positions, null tolerance |
| `data/AppRepositoryLogicPropertyTest` | alphabetical ordering, change detection (install / rename / identical list) |
| `AppFcmServicePropertyTest` | FCM token persistence, exceptions contained |
| `NotificationHelperPropertyTest` | notification title/body fallbacks |
| `DataPayloadHandlerPropertyTest` | exclusive payload routing, unknown payloads discarded |

Run: `./gradlew :app:testDebugUnitTest` — and `./gradlew :app:assembleDebug` for the APK.

---

## 9. Known remaining debt

- The manifest components still share one flat `Launcher` package (rule 1). A physical
  split requires either keeping `android:name` aliases valid or accepting that existing
  installs lose the home-app role and the accessibility grant — do not do it silently.
- Reward codes are signed with a key that is hardcoded in the APK, so they are forgeable.
- Purchases are consumed and validated on-device only (no server verification).
- `isMinifyEnabled = false` for release; enabling it needs keep rules for Gson models,
  Billing and Firebase first.
- `versionCode`/`versionName` are still `21`; bump before uploading a fixed build.

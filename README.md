# 📱 Addiction Control – Best Android App to Reduce Phone Addiction

**Addiction Control** is a powerful and minimalist **Android launcher** that helps you break free from smartphone addiction. It automatically **blocks distracting apps** and only allows access to your **user-approved apps list**. With an optional **one-time in-app purchase**, users can unlock full access while maintaining digital discipline.

👉 **[Download on Google Play](https://play.google.com/store/apps/details?id=com.genzopia.addiction&pcampaignid=web_share)**

---

## ✨ Key Features of Addiction Control

- 🚫 **App Blocker** – Automatically force-stops unapproved apps to control usage.
- 🔐 **Unlock All Apps** – Make a one-time purchase to remove all app restrictions.
- ⏳ **Smart Time Limit Storage** – Save control settings and app usage limits.
- 🧘 **Minimalist Home Launcher** – Replace your Android home screen with a distraction-free launcher.
- 🔄 **Persistent Settings** – Your preferences stay saved even after device reboots.

---

## 📦 Project Structure (for Developers)

📖 **Full map of the codebase: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)** — screen flow,
package layout, the app-list data layer, every SharedPreferences key, the enforcement engine
and the rules that must not be broken when refactoring (most importantly: **never rename a
class declared in `AndroidManifest.xml`** — the default-launcher choice and the granted
accessibility permission are keyed on the component name).

| Component | Purpose |
|----------|---------|
| `data.AppRepository` | Single source of truth for the installed-app list; auto-refreshes on install/uninstall |
| `ui.common.AppListAdapter` | The one adapter used by every app list (drawer, locked drawer, shortcut picker) |
| `PopupActivity` | Displays a blocking dialog when a restricted app is opened |
| `MainContainerActivity` | Custom launcher screen (replaces the default home screen) |
| `NotificationBarDetectorService` | Accessibility service that detects and interrupts blocked apps |
| `SharedPrefHelper` | Manages app blocking and timer preferences locally |
| `BillingActivity` | Handles in-app purchases like `unlock_discipline_lock_v2` |

---

## 🛠️ Getting Started

### 📋 Requirements

- Android Studio Arctic Fox (or newer)
- Android SDK version 24 and above
- Google Play Console account (for testing in-app purchases)

### 🚀 Installation Steps

```bash
git clone https://github.com/hk973/Addiction_Control.git
cd addiction-control

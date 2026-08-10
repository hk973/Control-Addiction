package com.genzopia.addiction.Launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.content.ContextCompat;

import com.genzopia.addiction.R;

import java.util.ArrayList;

public class NotificationBarDetectorService extends AccessibilityService {
    private final ArrayMap<String, Boolean> mAppValidityCache = new ArrayMap<>();
    private volatile String mLockClassName;
    private volatile boolean mIsAuthenticating;
    private boolean isPollingAppInfo = false;

    /** Interval of the anti-tamper node-tree scan. */
    private static final long POLL_INTERVAL_MS = 400L;
    /** Hard depth limit for the recursive node scan so a deep tree can never stall a thread. */
    private static final int MAX_NODE_DEPTH = 12;

    private HandlerThread workerThread;
    private Handler workerHandler;
    private Handler mainHandler;
    private Runnable pollingRunnable;
    private PowerManager powerManager;
    private volatile boolean isScreenOn = true;
    private boolean isReceiverRegistered = false;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Runs on the worker thread (see registerReceiver below), so the disk reads
            // done here can never block the main thread while the broadcast is dispatched.
            final String action = intent == null ? null : intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                isScreenOn = true;
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                isScreenOn = false;
                stopPolling();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        mainHandler = new Handler(Looper.getMainLooper());
        workerThread = new HandlerThread("BlockerWorker");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        isScreenOn = powerManager == null || powerManager.isInteractive();

        AuthenticationManager.getInstance().addListener(newState -> {
            mIsAuthenticating = (newState == Authentication.going);
        });

        // One filter for both protected system actions, delivered on the worker thread.
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        try {
            ContextCompat.registerReceiver(this, screenReceiver, screenFilter, null,
                    workerHandler, ContextCompat.RECEIVER_NOT_EXPORTED);
            isReceiverRegistered = true;
        } catch (Exception e) {
            Log.e("AccessibilityService", "Failed to register screen receiver", e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            SharedPrefHelper prefHelper = new SharedPrefHelper(this);

            // ── Scheduled blocking check ───────────────────────────────────
            if (!prefHelper.getTimeActivateStatus()
                    && ScheduledBlockingManager.shouldBeActiveNow(this)) {
                ScheduledBlockingManager.ScheduleEntry active =
                        ScheduledBlockingManager.getActiveSchedule(this);
                if (active != null && active.durationMinutes > 0) {
                    long durationSec = active.durationMinutes * 60L;
                    prefHelper.saveStartTime(System.currentTimeMillis());
                    prefHelper.saveInitialDuration(durationSec);
                    prefHelper.saveTimeActivateStatus(true);
                }
            }

            if (!prefHelper.getTimeActivateStatus()) return;

            final String pkg = String.valueOf(event.getPackageName());
            final String className = event.getClassName().toString();
            final int eventType = event.getEventType();

            if (mIsAuthenticating) {
                mLockClassName = className;
                mIsAuthenticating = false;
            }

            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                handleWindowChange(pkg, className, prefHelper);
            }

            // Restart the anti-tamper poll on every window/content change —
            // previously isPollingAppInfo was never reset so polling stopped after first cycle.
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

                if (isScreenOn && workerHandler != null) {
                    // Cancel any pending poll and start a fresh one for this window.
                    stopPolling();
                    isPollingAppInfo = true;

                    final String appName = getString(R.string.app_name);
                    pollingRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!isScreenOn || !prefHelper.getTimeActivateStatus()) {
                                isPollingAppInfo = false;
                                return;
                            }

                            AccessibilityNodeInfo rootNode = null;
                            try {
                                rootNode = getRootInActiveWindow();
                                if (rootNode != null && isAppInfoScreen(rootNode, appName)) {
                                    triggerBlockingPopup();
                                }
                            } catch (Exception e) {
                                Log.e("AccessibilityService", "Node scan failed", e);
                            } finally {
                                if (rootNode != null) rootNode.recycle();
                            }

                            if (workerHandler != null) {
                                workerHandler.postDelayed(this, POLL_INTERVAL_MS);
                            }
                        }
                    };
                    workerHandler.post(pollingRunnable);
                }
            }

        } catch (Exception e) {
            Log.e("AccessibilityService", "Exception in onAccessibilityEvent", e);
        }
    }

    private void handleWindowChange(String pkg, String className, SharedPrefHelper prefHelper) {
        ArrayList<String> allowedApps = prefHelper.getSelectedAppValue();

        if (allowedApps == null || allowedApps.contains(pkg)) return;

        // Only skip once for the exact authenticated class, then clear it
        if (className.equals(mLockClassName)) {
            mLockClassName = null;
            return;
        }

        String classNameLower = className.toLowerCase();
        if (className.contains("com.android.settings.password.ConfirmDeviceCredentialActivity") ||
                classNameLower.contains("confirmdevicecredential") ||
                classNameLower.contains("keyguard") ||
                classNameLower.contains("password") ||
                classNameLower.contains("pin") ||
                classNameLower.contains("pattern") ||
                classNameLower.contains("lock") ||
                classNameLower.contains("security") ||
                classNameLower.contains("biometric") ||
                classNameLower.contains("fingerprint")) return;

        if (isPredefinedSystemApp(pkg)) {
            if (!prefHelper.appWithNoWarning().contains(pkg)) {
                triggerBlockingPopup();
            }
            return;
        }

        if (isValidApplication(pkg) && !prefHelper.appWithNoWarning().contains(pkg)) {
            triggerBlockingPopup();
        }
    }

    private boolean isAppInfoScreen(AccessibilityNodeInfo rootNode, String yourAppName) {
        // Short-circuit with && so the second, usually pointless tree walk is skipped.
        return containsKeyword(rootNode, 0, "uninstall", "force stop", "clear data",
                "clear cache", "accessibility", "talkback")
                && containsKeyword(rootNode, 0, yourAppName);
    }

    private boolean containsKeyword(AccessibilityNodeInfo node, int depth, String... keywords) {
        if (node == null || depth > MAX_NODE_DEPTH) return false;

        CharSequence text = node.getText();
        if (text != null) {
            String lowerText = text.toString().toLowerCase();
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                if (containsKeyword(child, depth + 1, keywords)) {
                    return true;
                }
            } finally {
                // Nodes obtained from getChild() must be released or the service leaks them.
                child.recycle();
            }
        }

        return false;
    }

    private boolean isPredefinedSystemApp(String pkg) {
        return pkg.equals("com.android.settings") ||
                pkg.equals("com.miui.securitycenter") ||
                pkg.equals("com.android.vending") ||
                pkg.equals("com.google.android.youtube") ||
                pkg.equals("com.android.chrome") ||
                pkg.equals("com.android.intentresolver");
    }

    private boolean isValidApplication(String pkg) {
        Boolean cached = mAppValidityCache.get(pkg);
        if (cached != null) return cached;

        boolean isValid = false;
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
            isValid = info.enabled && ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        mAppValidityCache.put(pkg, isValid);
        return isValid;
    }
    private static NotificationBarDetectorService instance;

    public static NotificationBarDetectorService getInstance() {
        return instance;
    }

    /** Minimum gap between two blocking popups. */
    private static final long BLOCK_TRIGGER_THROTTLE_MS = 2000L;
    private volatile long lastBlockTriggerMs = 0L;

    private void triggerBlockingPopup() {
        // The poll loop can detect the same screen many times in a row; starting the
        // overlay + activity on every hit floods the window manager and ends up as an
        // "Input dispatching timed out (No focused window)" ANR.
        long now = System.currentTimeMillis();
        if (now - lastBlockTriggerMs < BLOCK_TRIGGER_THROTTLE_MS) return;
        lastBlockTriggerMs = now;

        // Award XP / update streak for every block event
        GamificationManager.onAppBlocked(getApplicationContext());

        Handler handler = mainHandler != null ? mainHandler : new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (Settings.canDrawOverlays(this)) {
                try {
                    startService(new Intent(this, OverlayService.class));
                } catch (Exception e) {
                    Log.e("PopupTriggerError", "Failed to show overlay: " + e.getMessage(), e);
                }
            } else {
                // No overlay permission — silently skip
                Log.w("PopupTrigger", "Overlay permission not granted. Skipping popup.");
            }

            try {
                startActivity(new Intent(this, PopupActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Log.e("PopupTriggerError", "Failed to show popup activity", e);
            }
        });
    }

    private void stopPolling() {
        if (workerHandler != null && pollingRunnable != null) {
            workerHandler.removeCallbacks(pollingRunnable);
        }
        isPollingAppInfo = false;
    }
    public void goToHomeScreen() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }
    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (IllegalArgumentException ignored) {
                // Already unregistered.
            }
            isReceiverRegistered = false;
        }
        stopPolling();
        if (workerThread != null) {
            // quitSafely() does not block the caller, unlike quit() + join().
            workerThread.quitSafely();
            workerThread = null;
            workerHandler = null;
        }
        instance = null;
        Log.d("accessibilty_test", "Service DESTROYED");
    }

    @Override
    public void onInterrupt() {
        Log.d("accessibilty_test", "Service INTERRUPTED");
    }
}

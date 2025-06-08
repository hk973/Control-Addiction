package com.genzopia.addiction;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.ArrayMap;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;

public class NotificationBarDetectorService extends AccessibilityService {
    private final ArrayMap<String, Boolean> mAppValidityCache = new ArrayMap<>();
    private volatile String mLockClassName;
    private volatile boolean mIsAuthenticating;

    @Override
    public void onCreate() {
        super.onCreate();
        AuthenticationManager.getInstance().addListener(newState -> {
            mIsAuthenticating = (newState == Authentication.going);
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            SharedPrefHelper prefHelper = new SharedPrefHelper(this);
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
        } catch (Exception e) {
            Log.e("AccessibilityService", "Exception in onAccessibilityEvent", e);
        }
    }

    private void handleWindowChange(String pkg, String className, SharedPrefHelper prefHelper) {
        ArrayList<String> allowedApps = prefHelper.getSelectedAppValue();

        if (allowedApps == null || allowedApps.contains(pkg)) return;
        if (className.equals(mLockClassName)) return;

        // Fast-check common system apps
        if (isPredefinedSystemApp(pkg)) {
            if (!prefHelper.appWithNoWarning().contains(pkg)) {
                triggerBlockingPopup();
            }
            return;
        }

        // Check app validity with caching
        if (isValidApplication(pkg) && !prefHelper.appWithNoWarning().contains(pkg)) {
            triggerBlockingPopup();
        }

        // Handle recents screen
        if (className.contains("RecentsActivity")) {
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    }

    private boolean isPredefinedSystemApp(String pkg) {
        return pkg.equals("com.android.settings") ||
                pkg.equals("com.android.vending") ||
                pkg.equals("com.google.android.youtube") ||
                pkg.equals("com.android.chrome") ||
                pkg.equals("com.android.intentresolver");
    }

    private boolean isValidApplication(String pkg) {
        // Check cache first
        Boolean cached = mAppValidityCache.get(pkg);
        if (cached != null) return cached;

        // Resolve if not cached
        boolean isValid = false;
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
            isValid = info.enabled &&
                    ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        mAppValidityCache.put(pkg, isValid);
        return isValid;
    }

    private void triggerBlockingPopup() {
        startActivity(new Intent(this, PopupActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    protected void onServiceConnected() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("AccessibilityCrash", "CRASH: " + Log.getStackTraceString(throwable));
            // Restart service after crash
            Intent intent = new Intent(this, NotificationBarDetectorService.class);
            startService(intent);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("accessibilty_test", "Service DESTROYED");
    }

    @Override
    public void onInterrupt() {
        Log.d("accessibilty_test", "Service INTERRUPTED");
    }
}
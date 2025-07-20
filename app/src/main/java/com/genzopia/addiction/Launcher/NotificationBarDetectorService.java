package com.genzopia.addiction.Launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.ArrayMap;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.genzopia.addiction.R;

import java.util.ArrayList;

public class NotificationBarDetectorService extends AccessibilityService {
    private final ArrayMap<String, Boolean> mAppValidityCache = new ArrayMap<>();
    private volatile String mLockClassName;
    private volatile boolean mIsAuthenticating;
    private boolean isPollingAppInfo = false;

    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private PowerManager powerManager;
    private boolean isScreenOn = true;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                isScreenOn = true;
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                isScreenOn = false;
                stopPolling();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        pollingHandler = new Handler(Looper.getMainLooper());
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        isScreenOn = powerManager.isInteractive();

        AuthenticationManager.getInstance().addListener(newState -> {
            mIsAuthenticating = (newState == Authentication.going);
        });

        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
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

            if ((eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) {

                if (!isPollingAppInfo && isScreenOn) {
                    isPollingAppInfo = true;

                    pollingRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!isScreenOn) {
                                isPollingAppInfo = false;
                                return;
                            }

                            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                            if (rootNode != null && isAppInfoScreen(rootNode, getString(R.string.app_name))) {
                                Log.d("BLOCKER", "User is in App Info");
                                triggerBlockingPopup();
                            }

                            pollingHandler.postDelayed(this, 120);
                        }
                    };

                    pollingHandler.post(pollingRunnable);
                }
            }

        } catch (Exception e) {
            Log.e("AccessibilityService", "Exception in onAccessibilityEvent", e);
        }
    }

    private void handleWindowChange(String pkg, String className, SharedPrefHelper prefHelper) {
        ArrayList<String> allowedApps = prefHelper.getSelectedAppValue();
        Log.e("test99", pkg + className);

        if (allowedApps == null || allowedApps.contains(pkg)) return;
        if (className.equals(mLockClassName)) return;

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
                Log.e("test999", pkg + className);
                triggerBlockingPopup();
            }
            return;
        }

        if (isValidApplication(pkg) && !prefHelper.appWithNoWarning().contains(pkg)) {
            Log.e("test9999", pkg + className);
            triggerBlockingPopup();
        }

//        if (className.contains("RecentsActivity")) {
//            performGlobalAction(GLOBAL_ACTION_HOME);
//        }
    }

    private boolean isAppInfoScreen(AccessibilityNodeInfo rootNode, String yourAppName) {
        boolean foundDangerousAction = containsKeyword(rootNode, "uninstall", "force stop","clear data","clear cache");
        boolean foundAppName = containsKeyword(rootNode, yourAppName);
        Log.e("testinfoscreen", "action=" + foundDangerousAction + " appname=" + foundAppName);
        return foundAppName & foundDangerousAction;
    }

    private boolean containsKeyword(AccessibilityNodeInfo node, String... keywords) {
        if (node == null) return false;

        CharSequence text = node.getText();
        if (text != null) {
            String lowerText = text.toString().toLowerCase();
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (containsKeyword(node.getChild(i), keywords)) {
                return true;
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

    private void triggerBlockingPopup() {
        startActivity(new Intent(this, PopupActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
        isPollingAppInfo = false;
    }

    @Override
    protected void onServiceConnected() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("AccessibilityCrash", "CRASH: " + Log.getStackTraceString(throwable));
            Intent intent = new Intent(this, NotificationBarDetectorService.class);
            startService(intent);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenReceiver);
        stopPolling();
        Log.d("accessibilty_test", "Service DESTROYED");
    }

    @Override
    public void onInterrupt() {
        Log.d("accessibilty_test", "Service INTERRUPTED");
    }
}

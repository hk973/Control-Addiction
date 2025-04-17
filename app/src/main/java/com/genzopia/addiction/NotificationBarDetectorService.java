package com.genzopia.addiction;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;

public class NotificationBarDetectorService extends AccessibilityService {
    private ArrayList<String> selectedApps;
    private SharedPrefHelper sharedPrefHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPrefHelper = new SharedPrefHelper(getApplicationContext());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        boolean timeActive = sharedPrefHelper.getTimeActivateStatus();
        String currentPackage = String.valueOf(event.getPackageName());
       Log.e("test9000",currentPackage);
        if (timeActive) {
            selectedApps = sharedPrefHelper.getSelectedAppValue();
            int eventType = event.getEventType();
            String className = event.getClassName().toString();

            // Only respond to window state changes in applications
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Log.d("AppDetection", "Active package: " + currentPackage);


                // Check if the current app is not in the allowed list
                if (!selectedApps.contains(currentPackage)) {
                    boolean isApp=isValidApplication(currentPackage);
                    String package_i_want_tobe_app="com.android.settings";
                    String i_want="com.android.vending";
                    String youtube="com.google.android.youtube";
                    String chrome="com.android.chrome";
                    String interresolver="com.android.intentresolver";

                    if(currentPackage.equals(package_i_want_tobe_app)||currentPackage.equals(i_want)||currentPackage.equals(youtube)||currentPackage.equals(chrome)||currentPackage.equals(interresolver)){
                        isApp=true;
                    }
                    if(isApp&&!sharedPrefHelper.appwithnowarning().contains(currentPackage)){
                    Log.d("AppDetection", "Blocking non-selected app: " + currentPackage);
                    triggerBlockingPopup();
                    }
                    if (className.contains("RecentsActivity")) {
                            performGlobalAction(GLOBAL_ACTION_HOME);
                    }
                }
            }


        }
    }
    private boolean isValidApplication(String packageName) {
        try {
           PackageManager packageManager = getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return appInfo.enabled && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false; // Not a valid application
        }
    }

    private void triggerBlockingPopup() {
        Intent popupIntent = new Intent(this, PopupActivity.class);
        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(popupIntent);
    }

    @Override
    public void onInterrupt() {
        // Required override
    }
}
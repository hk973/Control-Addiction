package com.genzopia.addiction;

import android.app.Activity;
import android.util.Log;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

public class AppUpdateChecker implements DefaultLifecycleObserver {
    private static final String TAG = "AppUpdateChecker";
    private static final int REQUEST_CODE = 1024; // Unique value

    private final Activity activity;
    private boolean updateChecked = false;

    public AppUpdateChecker(Activity activity) {
        this.activity = activity;
        Log.d(TAG, "Initialized for activity: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onStart(LifecycleOwner owner) {
        Log.d(TAG, "onStart() called, updateChecked: " + updateChecked);
        if (!updateChecked) {
            Log.i(TAG, "Starting update check...");
            checkForUpdate();
            updateChecked = true;
        }
    }

    private void checkForUpdate() {
        try {
            AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
            Log.d(TAG, "Created AppUpdateManager instance");

            appUpdateManager.getAppUpdateInfo()
                    .addOnSuccessListener(appUpdateInfo -> {
                        logUpdateInfo(appUpdateInfo);
                        handleUpdateAvailability(appUpdateManager, appUpdateInfo);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Update check failed: " + e.getMessage(), e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Critical error in checkForUpdate: " + e.getMessage(), e);
        }
    }

    private void logUpdateInfo(AppUpdateInfo appUpdateInfo) {
        Log.d(TAG, "Update check succeeded. Version code: " + appUpdateInfo.availableVersionCode()
                + "\nAvailability: " + appUpdateInfo.updateAvailability()
                + "\nImmediate allowed: " + appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                + "\nFlexible allowed: " + appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE));
    }

    private void handleUpdateAvailability(AppUpdateManager appUpdateManager, AppUpdateInfo appUpdateInfo) {
        try {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                Log.i(TAG, "Update available detected");

                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    Log.i(TAG, "Immediate update allowed, attempting flow start");
                    startUpdateFlow(appUpdateManager, appUpdateInfo);
                } else {
                    Log.w(TAG, "Immediate update not allowed");
                }
            } else {
                Log.i(TAG, "No update available. Availability code: "
                        + appUpdateInfo.updateAvailability());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling update availability: " + e.getMessage(), e);
        }
    }

    private void startUpdateFlow(AppUpdateManager appUpdateManager, AppUpdateInfo appUpdateInfo) {
        if (isActivityValid()) {
            try {
                Log.i(TAG, "Starting update flow...");
                appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        activity,
                        REQUEST_CODE
                );
                Log.i(TAG, "Update flow started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start update flow: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "Cannot start update flow - activity not valid");
        }
    }

    private boolean isActivityValid() {
        boolean valid = !activity.isFinishing() && !activity.isDestroyed();
        Log.d(TAG, "Activity valid check: " + valid);
        return valid;
    }
}
package com.genzopia.addiction.Launcher;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper for requesting the POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+).
 * Requirements: 2.1, 2.2, 2.3
 */
public class NotificationPermissionHelper {

    private static final String TAG = "NotifPermHelper";

    /** Request code used when calling ActivityCompat.requestPermissions. */
    public static final int REQUEST_CODE = 1001;

    /**
     * Requests the POST_NOTIFICATIONS permission if running on API 33+ and not yet granted.
     * On API < 33 this method returns immediately without any action.
     *
     * Requirement 2.1
     *
     * @param activity The calling Activity used to check and request the permission.
     */
    public static void requestIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Notifications are allowed implicitly below API 33 — nothing to request.
            Log.d(TAG, "API < 33; POST_NOTIFICATIONS permission not required.");
            return;
        }

        int status = ContextCompat.checkSelfPermission(
                activity, Manifest.permission.POST_NOTIFICATIONS);

        if (status == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "POST_NOTIFICATIONS already granted.");
        } else {
            Log.d(TAG, "Requesting POST_NOTIFICATIONS permission.");
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE);
        }
    }
}

package com.genzopia.addiction.Launcher;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Map;

/**
 * Routes FCM data payloads to the appropriate action handler based on the {@code type} field.
 *
 * Requirements: 1.3, 1.4, 1.5, 2.5, 2.6, 3.1, 3.2, 3.3
 */
public class DataPayloadHandler {

    private static final String TAG = "DataPayloadHandler";

    /** Local broadcast action consumed by MainContainerActivity. */
    public static final String ACTION_FORCE_UPDATE =
            "com.genzopia.addiction.ACTION_FORCE_UPDATE";

    /** Known {@code type} field values in the FCM data payload. */
    public static final String TYPE_SHOW_NOTIFICATION = "show_notification";
    public static final String TYPE_FORCE_UPDATE      = "force_update";
    public static final String TYPE_CONFIG_REFRESH    = "config_refresh";

    /**
     * Delegate interface used to decouple handler methods from the static entry point.
     * The default production implementation is {@link DefaultDelegate}.
     * Tests may supply a custom delegate to observe dispatch without needing a real context.
     */
    public interface Delegate {
        void onShowNotification(Context context, Map<String, String> data);
        void onForceUpdate(Context context);
        void onConfigRefresh(Context context, Map<String, String> data);
    }

    /** Holds the active delegate; swappable for testing. */
    private static Delegate delegate = new DefaultDelegate();

    /** Replace the delegate (test use only). */
    public static void setDelegate(Delegate d) {
        delegate = d;
    }

    /** Reset to the production delegate. */
    public static void resetDelegate() {
        delegate = new DefaultDelegate();
    }

    /**
     * Entry point called by AppFcmService.
     *
     * Returns immediately (with a WARN log) if the map is null or empty.
     * Switches on the {@code type} field; logs WARN and returns for unknown/absent type.
     *
     * Requirements: 1.3, 1.4, 1.5
     */
    public static void handle(Context context, Map<String, String> data) {
        // Requirement 1.3 — null or empty map guard
        if (data == null || data.isEmpty()) {
            Log.w(TAG, "Received null or empty data map — ignoring message.");
            return;
        }

        String type = data.get("type");

        // Requirement 1.5 — absent or unknown type
        if (type == null || type.isEmpty()) {
            Log.w(TAG, "FCM data map missing 'type' field — discarding message.");
            return;
        }

        // Requirement 1.4 — route to the matching handler via the delegate
        switch (type) {
            case TYPE_SHOW_NOTIFICATION:
                delegate.onShowNotification(context, data);
                break;
            case TYPE_FORCE_UPDATE:
                delegate.onForceUpdate(context);
                break;
            case TYPE_CONFIG_REFRESH:
                delegate.onConfigRefresh(context, data);
                break;
            default:
                Log.w(TAG, "Unrecognized FCM message type '" + type + "' — discarding.");
        }
    }

    // -----------------------------------------------------------------------
    // Production delegate implementation
    // -----------------------------------------------------------------------

    /** Default production delegate — performs real side effects. */
    public static class DefaultDelegate implements Delegate {

        /**
         * Handles {@code type = "show_notification"}.
         * Extracts title, body, and optional image_url from the data map.
         * Image download is done on a background thread to avoid blocking.
         *
         * Default title : "Notification"
         * Default body  : "You have a new message"
         *
         * Requirements: 2.5, 2.6
         */
        @Override
        public void onShowNotification(Context context, Map<String, String> data) {
            String title    = NotificationHelper.applyTitleDefault(data.get("title"));
            String body     = NotificationHelper.applyBodyDefault(data.get("body"));
            String imageUrl = data.get("image_url");
            boolean silent  = "true".equalsIgnoreCase(data.get("silent"));

            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // Download image off the main thread
                new Thread(() ->
                        NotificationHelper.show(context, title, body, imageUrl, silent, null)
                ).start();
            } else {
                NotificationHelper.show(context, title, body, null, silent, null);
            }
        }

        /**
         * Handles {@code type = "force_update"}.
         * Sends a local broadcast with action {@link DataPayloadHandler#ACTION_FORCE_UPDATE}.
         * MainContainerActivity registers a receiver for this action.
         *
         * Requirements: 3.1, 3.3
         */
        @Override
        public void onForceUpdate(Context context) {
            Intent intent = new Intent(ACTION_FORCE_UPDATE);
            context.sendBroadcast(intent);
            Log.d(TAG, "Sent ACTION_FORCE_UPDATE broadcast.");
        }

        /**
         * Handles {@code type = "config_refresh"}.
         * Reads {@code config_key} and {@code config_value} from the data map
         * and writes them to {@link SharedPrefHelper}.
         *
         * Requirement: 3.2
         */
        @Override
        public void onConfigRefresh(Context context, Map<String, String> data) {
            String key   = data.get("config_key");
            String value = data.get("config_value");

            if (key == null || key.trim().isEmpty()) {
                Log.w(TAG, "config_refresh: missing 'config_key' — ignoring.");
                return;
            }
            if (value == null) {
                Log.w(TAG, "config_refresh: missing 'config_value' for key '"
                        + key + "' — ignoring.");
                return;
            }

            SharedPrefHelper helper = new SharedPrefHelper(context);
            helper.saveString(context, key, value);
            Log.d(TAG, "config_refresh: wrote key='" + key + "'.");
        }
    }
}

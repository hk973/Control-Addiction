package com.genzopia.addiction.Launcher;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import android.content.Context;

/**
 * Handles incoming FCM data messages and token lifecycle events.
 *
 * Requirements: 1.1, 1.2, 1.3, 4.1, 4.2, 4.4
 */
public class AppFcmService extends FirebaseMessagingService {

    private static final String TAG = "AppFcmService";
    private static final String FCM_ERROR_TAG = "FCM_ERROR";

    /**
     * Called when a new FCM registration token is generated or refreshed.
     *
     * Saves the token to SharedPreferences under the key {@code fcm_token}.
     * Also triggers a fresh token fetch to ensure we always have the latest.
     *
     * Requirement 1.1
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed.");
        SharedPrefHelper helper = new SharedPrefHelper(this);
        helper.saveString(this, "fcm_token", token);
    }

    /**
     * Called when an FCM message is received.
     *
     * Handles two cases:
     * 1. Notification message (sent from dashboard Notification/Combined tab) —
     *    extracts title and body from RemoteMessage.getNotification() and posts
     *    a local notification via NotificationHelper.
     * 2. Data-only message (sent from dashboard Data tab) —
     *    delegates to DataPayloadHandler for type-based routing.
     *
     * When both a notification object and a data map are present (Combined tab),
     * the notification is shown AND the data payload is processed.
     *
     * Requirements: 1.2, 1.3, 4.1, 4.4
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        // Handle notification payload (from Notification or Combined tab)
        RemoteMessage.Notification notification = message.getNotification();
        if (notification != null) {
            String title = NotificationHelper.applyTitleDefault(notification.getTitle());
            String body  = NotificationHelper.applyBodyDefault(notification.getBody());
            NotificationHelper.show(this, title, body, null);
            Log.d(TAG, "Displayed FCM notification: title=" + title);
        }

        // Handle data payload (from Data or Combined tab)
        if (!message.getData().isEmpty()) {
            processPayload(this, message.getData());
        }
    }

    /**
     * Extracted processing logic to allow unit testing without a real RemoteMessage.
     * Wraps all processing in a try-catch so no exception escapes to the Android runtime.
     *
     * Requirements: 1.2, 1.3, 4.1, 4.4
     *
     * @param context Application context (may be null in tests when the delegate is a spy)
     * @param data    FCM data map from RemoteMessage.getData()
     */
    public static void processPayload(android.content.Context context, Map<String, String> data) {
        try {
            // Requirement 1.3 — guard against empty payloads
            if (data == null || data.isEmpty()) {
                Log.w(TAG, "Received FCM message with null or empty data map — ignoring.");
                return;
            }

            // Requirement 1.2 — delegate all processing to DataPayloadHandler
            DataPayloadHandler.handle(context, data);

        } catch (Exception e) {
            // Requirements 4.1, 4.4 — catch any exception, log it, do not rethrow
            Log.e(FCM_ERROR_TAG, "Exception during FCM message processing: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches the current FCM token on service start.
     * Logs the failure cause if retrieval fails (token will be retried by the Firebase SDK).
     *
     * Requirement 4.2
     */
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "FCM token fetched on start.");
                    SharedPrefHelper helper = new SharedPrefHelper(this);
                    helper.saveString(this, "fcm_token", token);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "FCM token fetch failed: " + e.getMessage() + ". Will retry on next start.", e)
                );
    }
}

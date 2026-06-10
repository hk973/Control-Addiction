package com.genzopia.addiction;

import android.app.Application;
import android.util.Log;

import com.genzopia.addiction.Launcher.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessaging;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Create the notification channel as early as possible so FCM messages
        // delivered while the app is killed or in background are never dropped.
        NotificationHelper.createChannel(this);

        // Subscribe to broadcast topic so all installs receive FCM notifications
        // from the Firebase console. Runs once per process start — Firebase SDK
        // de-duplicates repeated subscriptions automatically.
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Topic subscription failed: " + task.getException());
                    } else {
                        Log.d("FCM", "Subscribed to topic: all_users");
                    }
                });
    }
}

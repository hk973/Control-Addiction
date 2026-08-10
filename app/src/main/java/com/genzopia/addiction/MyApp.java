package com.genzopia.addiction;

import android.app.Application;
import android.util.Log;

import com.genzopia.addiction.Launcher.NotificationHelper;
import com.genzopia.addiction.data.AppRepository;
import com.google.firebase.messaging.FirebaseMessaging;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Create the notification channel as early as possible so FCM messages
        // delivered while the app is killed or in background are never dropped.
        NotificationHelper.createChannel(this);

        // Single source of truth for every app list. Registering the package receiver here
        // is what makes a newly installed app show up in the drawer without a restart —
        // the launcher process can stay alive for days, so a one-off load is not enough.
        AppRepository repository = AppRepository.getInstance(this);
        repository.registerPackageReceiver();
        repository.refresh();

        // Subscribe to broadcast topic so all installs receive FCM notifications
        // from the Firebase console. Runs once per process start — Firebase SDK
        // de-duplicates repeated subscriptions automatically.
        //
        // FirebaseMessaging.getInstance() forces Firebase component discovery, which is
        // slow enough on cold start to stall the main thread (seen as an ANR inside
        // FirebaseCommonRegistrar.getComponents), so it is done off the main thread.
        Thread initThread = new Thread(() -> {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.w("FCM", "Topic subscription failed: " + task.getException());
                            } else {
                                Log.d("FCM", "Subscribed to topic: all_users");
                            }
                        });
            } catch (Exception e) {
                Log.w("FCM", "Topic subscription could not be started", e);
            }
        }, "FcmTopicInit");
        initThread.setDaemon(true);
        initThread.start();
    }
}

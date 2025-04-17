package com.genzopia.addiction;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;



public class ForegroundAppService extends Service {

    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private PackageManager packageManager;
    private ArrayList<String> selectedApps; // Store selected app package names
    private List<String> allowedApps; // List of apps that are allowed to run
    private SharedPrefHelper sharedPrefHelper;
    private int timeLimit; // Time limit in seconds
    private boolean isActive; // Timer active status
    private Thread timerThread; // Thread for timer management

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        packageManager = getPackageManager();
        sharedPrefHelper = new SharedPrefHelper(this); // Initialize SharedPrefHelper


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {




        // Retrieve the time limit and active status from SharedPreferences
        timeLimit = sharedPrefHelper.getTimeLimitValue(); // Get the saved time limit
        isActive = sharedPrefHelper.getTimeActivateStatus(); // Get the active status
        SharedPrefHelper sp = new SharedPrefHelper(this);
        sp.saveTimeActivateStatus(true);

        // Create and show the notification
        Intent notificationIntent = new Intent(this, MainActivity3.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoring Apps")
                .setContentText("Monitoring selected apps. Time remaining: " + timeLimit + " seconds")
                .setSmallIcon(R.drawable.ic_launcher_background) // Use your own notification icon
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);

        // Start the timer thread to check the foreground app and countdown periodically
        if (timerThread == null || !timerThread.isAlive()) {
            timerThread = new Thread(() -> {
                while (true) {
                    timeLimit = sharedPrefHelper.getTimeLimitValue(); // Get the saved time limit
                    isActive = sharedPrefHelper.getTimeActivateStatus(); // Get the active status
                    if (isActive && timeLimit > 0) {

                        timeLimit--; // Decrement the time limit
                        sharedPrefHelper.saveTimeLimitValue(timeLimit); // Save updated time limit
                        updateNotification(notification); // Update notification with remaining time

                        try {
                            Thread.sleep(1000); // Wait for 1 second
                        } catch (InterruptedException e) {
                            Log.e("ForegroundAppService", "Thread interrupted", e);
                        }
                    } else if (timeLimit <= 0) {
                        // Stop the service when the timer reaches zero
                        Log.e("ForegroundAppService", "Timer expired, stopping service");
                        stopSelf(); // Stop the service
                        SharedPrefHelper p = new SharedPrefHelper(this);
                        p.saveTimeActivateStatus(false);
                        break; // Exit the loop if the service has been stopped
                    }

                }
            });
            timerThread.start();
        }

        return START_STICKY; // Service will restart if killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding, return null
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setSound(null, null);  // Explicitly set sound to null to mute
            serviceChannel.setDescription("This channel is used by the ForegroundAppService");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void updateNotification(Notification notification) {
        Notification updatedNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoring Apps")
                .setContentText("Monitoring selected apps. Time remaining: " + formatTime(timeLimit))
                .setSmallIcon(R.drawable.app)
                .setContentIntent(notification.contentIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();

        startForeground(1, updatedNotification); // Update the existing notification
    }



    // Function to check if permission is granted
    private boolean isUsageStatsGranted() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // Function to direct user to settings if permission is not granted
    private void requestUsageStatsPermission() {
        if (!isUsageStatsGranted()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }



    public  String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}


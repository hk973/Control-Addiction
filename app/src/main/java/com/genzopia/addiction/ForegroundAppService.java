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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
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
    private long startTime; // Track when the timer started
    private int initialDuration; // Initial time limit in seconds

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        packageManager = getPackageManager();
        sharedPrefHelper = new SharedPrefHelper(this); // Initialize SharedPrefHelper

        // Initialize the list of allowed apps
        allowedApps = new ArrayList<>();
//        allowedApps.add("com.android.settings"); // Add Settings app
        allowedApps.add("com.android.systemui"); // Add System UI app
        allowedApps.add("android"); // Add Android system apps
        allowedApps.add("com.android.vending");

        // upi system
        allowedApps.add("com.google.android.apps.nbu.paisa.user");
        allowedApps.add("net.one97.paytm");
        allowedApps.add("com.phonepe.app");
        allowedApps.add("in.org.npci.upiapp");
        allowedApps.add("com.google.android.gms");
        // Add any other system apps you want to allow
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e("ForegroundAppService", "Received null Intent, initializing empty selectedApps list");
            selectedApps = new ArrayList<>();
        } else {
            selectedApps = intent.getStringArrayListExtra("selectedApps");
            if (selectedApps == null) {
                selectedApps = new ArrayList<>();
            }
        }
        isActive = sharedPrefHelper.getTimeActivateStatus();
        initialDuration = sharedPrefHelper.getTimeLimitValue();
        startTime = sharedPrefHelper.getStartTime();
        // If timer is active but no start time recorded, initialize
        if (isActive && startTime == 0) {
            startTime = SystemClock.elapsedRealtime();
            sharedPrefHelper.saveStartTime(startTime);
            sharedPrefHelper.saveInitialDuration(initialDuration);
        }

        // Add the service's package to selected apps as needed
        selectedApps.add("com.genzopia.addiction");
        Log.e("test100", String.valueOf(selectedApps));

        // Retrieve the time limit and active status from SharedPreferences
        timeLimit = sharedPrefHelper.getTimeLimitValue(); // Get the saved time limit
        isActive = sharedPrefHelper.getTimeActivateStatus(); // Get the active status
        SharedPrefHelper sp = new SharedPrefHelper(this);
        sp.saveTimeActivateStatus(true);

        // Create and show the notification
        Intent notificationIntent = new Intent(this, SelectedAppsFragment.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoring Apps")
                .setContentText("Monitoring selected apps. Time remaining: " + timeLimit + " seconds")
                .setSmallIcon(R.drawable.notif_tile) // Use your own notification icon
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);


        // Start the monitoring thread
        if (timerThread == null || !timerThread.isAlive()) {
            timerThread = new Thread(() -> {
                while (true) {
                    // Get current status from SharedPreferences
                    isActive = sharedPrefHelper.getTimeActivateStatus();
                    initialDuration = sharedPrefHelper.getInitialDuration();
                    startTime = sharedPrefHelper.getStartTime();

                    if (isActive && initialDuration > 0 && startTime != 0) {
                        long currentTime = SystemClock.elapsedRealtime();
                        long elapsedSeconds = (currentTime - startTime) / 1000;
                        int remainingTime = (int) (initialDuration - elapsedSeconds);

                        if (remainingTime > 0) {
                            updateNotification(remainingTime,notification);
                        } else {
                            // Time's up
                            handleTimerExpiration();
                            break;
                        }
                    } else {
                        // Timer not active or invalid values
                        break;
                    }

                    // Check every 5 seconds to save resources
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e("ForegroundAppService", "Thread interrupted", e);
                    }

                    checkForegroundApp();
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


    private void updateNotification(int remainingSeconds,Notification notification) {
        String formattedTime = formatTime(remainingSeconds);
        Notification updatedNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoring Apps")
                .setContentText("Time remaining: " + formattedTime)
                .setSmallIcon(R.drawable.app)
                .setContentIntent(notification.contentIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();

        startForeground(1, updatedNotification);
    }
    private void handleTimerExpiration() {
        // Stop service and clean up
        sharedPrefHelper.saveTimeActivateStatus(false);
        sharedPrefHelper.saveStartTime(0);
        sharedPrefHelper.saveInitialDuration(0);
        stopSelf();
    }


    private String getForegroundAppPackageName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();

            // Get usage stats for the last 10 seconds (you can adjust the duration as needed)
            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);

            if (usageStatsList != null && !usageStatsList.isEmpty()) {
                SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
                for (UsageStats usageStats : usageStatsList) {
                    sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (!sortedMap.isEmpty()) {
                    String packageName = sortedMap.get(sortedMap.lastKey()).getPackageName();
                    Log.e("test300",packageName);
                    return packageName; // Return the package name of the foreground app
                }
            }
        } else {
            // For devices below Lollipop, you can use the previous method
            ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
            if (activityManager != null) {
                List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        Log.e("test300",processInfo.processName);
                        return processInfo.processName;
                    }
                }
            }
        }
        return null; // No foreground app found
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

    private void checkForegroundApp() {
        try {
            String foregroundApp = getForegroundAppPackageName();
            Log.e("test456", foregroundApp);

            // Check if the foreground app is in the selected apps or allowed apps
            if (foregroundApp != null) {
                boolean isAllowed = allowedApps.contains(foregroundApp);
                boolean isSelected = selectedApps.contains(foregroundApp);
                boolean isApp = isValidApplication(foregroundApp); // Check if it is a valid app
                String package_i_want_tobe_app="com.android.settings";
                String i_want="com.android.vending";
                String youtube="com.google.android.youtube";
                String chrome="com.android.chrome";
                if(foregroundApp.equals(package_i_want_tobe_app)||foregroundApp.equals(i_want)||foregroundApp.equals(youtube)||foregroundApp.equals(chrome)){
                    isApp=true;
                }
                Log.e("test300",isAllowed+" "+isSelected+" "+isApp);

                // If the app is neither in the selected apps nor in the allowed apps, show the popup
                if (!isAllowed && !isSelected && isApp) {
                    // Get the app name from the package name
                    String appName = getApplicationName(foregroundApp);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        Intent popupIntent = new Intent(ForegroundAppService.this, PopupActivity.class);
                        popupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(popupIntent);
                    });

                }
            }
        } catch (Exception e) {
            Log.e("ForegroundAppService", "Error checking foreground app", e);
        }
    }

    private String getApplicationName(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return (String) packageManager.getApplicationLabel(appInfo);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return packageName; // Return package name if not found
        }
    }

    private boolean isValidApplication(String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return appInfo.enabled && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (NameNotFoundException e) {
            return false; // Not a valid application
        }
    }
    public  String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}


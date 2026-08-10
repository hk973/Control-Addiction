package com.genzopia.addiction.Launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.genzopia.addiction.R;
import com.genzopia.addiction.Launcher.NotificationPermissionHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainContainerActivity extends BaseActivity implements MainFragment.PinnedAppActionListener{
    public ViewPager2 viewPager;

    private AppUpdateChecker updateChecker;
    private static final int REQUEST_CODE = 123;
    private AppUpdateManager appUpdateManager;
    private static final int UPDATE_REQUEST_CODE = 123;

    /** Action broadcast by DataPayloadHandler when a force_update FCM message is received. */
    public static final String ACTION_FORCE_UPDATE = "com.genzopia.addiction.ACTION_FORCE_UPDATE";

    private ForceUpdateReceiver forceUpdateReceiver;

    /**
     * Single shared background executor for the short counter/review lookup.
     * It uses a daemon thread so it never keeps the process alive during shutdown —
     * a non-daemon thread created on every onStart() made the main thread wait for it
     * while exiting, which was reported as a "Slow exit" ANR.
     */
    private static final ExecutorService BACKGROUND_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread t = new Thread(runnable, "MainContainer-bg");
                t.setDaemon(true);
                return t;
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        viewPager.setUserInputEnabled(true);


        AppListViewModel viewModel = new ViewModelProvider(this).get(AppListViewModel.class);
        if (viewModel.getAppItemsLiveData().getValue() == null) {
            viewModel.loadApps(getApplicationContext());
        }

        SharedPrefHelper p = new SharedPrefHelper(this);
        if(p.getTimeLimitValue()<=0){
            p.saveTimeActivateStatus(false);}






        updateChecker = new AppUpdateChecker(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(updateChecker);
        applyAppTheme();

        NotificationHelper.createChannel(this);
    }
    private MainFragment getMainFragment() {
        if (viewPager.getCurrentItem() == 1) {
            Fragment fragment = getSupportFragmentManager()
                    .findFragmentByTag("f" + viewPager.getCurrentItem());

            if (fragment instanceof MainFragment) {
                return (MainFragment) fragment;
            }
        }
        return null;
    }
    // Apply grayscale effect to the root view if Gray Mode is enabled
    private void applyGrayScaleIfNeeded() {
        SharedPrefHelper sharedPrefHelper=new SharedPrefHelper(this);
        if (sharedPrefHelper != null && sharedPrefHelper.isGrayModeEnabled()) {
            // Get the root view of the activity
            View root = getWindow().getDecorView();
            root.setLayerType(View.LAYER_TYPE_HARDWARE, null); // Enable hardware layer for better performance

            // Create a ColorMatrix for grayscale effect
            Paint paint = new Paint();
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); // Set saturation to 0 for grayscale
            paint.setColorFilter(new ColorMatrixColorFilter(matrix));

            // Apply the color filter to the root view
            root.setLayerPaint(paint);
        }
    }

    private void handleCounterResult(int counte, boolean reviewShown) {
        if (isFinishing() || isDestroyed()) return;
        Log.e("test8767",String.valueOf(counte));


        if (counte == 10) {
            if (!reviewShown) showReviewDialog();
        } else if (counte == 5) {
            startActivity(new Intent(this, ReviewActivity.class));
        } else if (counte%5==0){
            Log.e("test8767",String.valueOf(true));
            checkForForceUpdate();

        }
    }

    private void showReviewDialog() {
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                ReviewDialog dialog = new ReviewDialog(this);
                dialog.show();
            }
        }, 2000); // Show after 2 seconds delay
    }

    private void applyAppTheme() {
        SharedPrefHelper prefs = new SharedPrefHelper(this);

        // 1. Figure out which AppCompatDelegate mode we actually want:
        int desiredMode;
        if (prefs.isGrayModeEnabled()) {
            // Gray implies “always night” so we can paint it gray later
            desiredMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (prefs.isFollowSystemThemeEnabled()) {
            desiredMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else if (prefs.isDarkModeEnabled()) {
            desiredMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            // explicit “light”
            desiredMode = AppCompatDelegate.MODE_NIGHT_NO;
        }

        // 2. Only change it if it’s not already applied:
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode != desiredMode) {
            AppCompatDelegate.setDefaultNightMode(desiredMode);
            // NOTE: this will cause an Activity recreation, so any code
            // after this in onCreate() will run again under the new mode.
        }

        // 3. Finally—after mode is settled—apply gray if requested:
        applyGrayScaleIfNeeded();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            // Update cancelled or failed — optionally close app
            finish();
        }
    }

    /**
     * Stores the POST_NOTIFICATIONS grant/deny result in SharedPreferences.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NotificationPermissionHelper.REQUEST_CODE) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            new SharedPrefHelper(this).setNotifPermissionGranted(this, granted);
        }
    }



    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 1) {
            MainFragment fragment = getMainFragment();
            if (fragment != null) {
                if (fragment.isTimeSet()) {
                    fragment.resetTimeSelection();
                    Toast.makeText(this, "Time selection reset", Toast.LENGTH_SHORT).show();
                } else {
                    viewPager.setCurrentItem(0);
                }
            } else {
                viewPager.setCurrentItem(0);
            }
        }
    }


    @Override
    public void showPinnedAppOptions(AppItem_Dataclass appItem) {
        // Get existing fragment from ViewPager adapter
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag("f" + viewPager.getCurrentItem());

        if (fragment instanceof MainFragment && isAdded()) {
            ((MainFragment) fragment).showPinnedAppOptionsSafe(appItem);
        }
    }
    private boolean isAdded() {
        return !isFinishing() && !isDestroyed();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private static class ScreenSlidePagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public ScreenSlidePagerAdapter(MainContainerActivity fa) {
            super(fa);
        }

        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            return position == 0 ? new HomeFragment() : new MainFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Register receiver for FCM force_update messages (Requirement 3.1).
        // The action is app-private, so the receiver MUST be registered as
        // RECEIVER_NOT_EXPORTED — starting with Android 14 (API 34) registering an
        // unexported-by-default receiver without an explicit flag throws SecurityException.
        try {
            forceUpdateReceiver = new ForceUpdateReceiver();
            IntentFilter filter = new IntentFilter(ACTION_FORCE_UPDATE);
            ContextCompat.registerReceiver(this, forceUpdateReceiver, filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED);
        } catch (Exception e) {
            // Never let receiver registration take the launcher down — without the
            // receiver we simply lose the push-triggered update check.
            forceUpdateReceiver = null;
            Log.e("MainContainerActivity", "Failed to register force-update receiver", e);
        }

        BACKGROUND_EXECUTOR.execute(() -> {
            try {
                CounterManager cm = new CounterManager();
                int counte = cm.increment(MainContainerActivity.this);
                boolean reviewShown = cm.getReview(MainContainerActivity.this);
                runOnUiThread(() -> handleCounterResult(counte, reviewShown));
            } catch (Exception e) {
                Log.e("MainContainerActivity", "Counter update failed", e);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (forceUpdateReceiver != null) {
            try {
                unregisterReceiver(forceUpdateReceiver);
            } catch (IllegalArgumentException ignored) {
                // Receiver was never registered — nothing to do.
            }
            forceUpdateReceiver = null;
        }
    }
    /**
     * Inner BroadcastReceiver that listens for ACTION_FORCE_UPDATE broadcasts
     * sent by DataPayloadHandler when a force_update FCM data message is received.
     * Requirement 3.1
     */
    private class ForceUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_FORCE_UPDATE.equals(intent.getAction())) {
                checkForForceUpdate();
            }
        }
    }

    private void checkForForceUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this);

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            UPDATE_REQUEST_CODE
                    );
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
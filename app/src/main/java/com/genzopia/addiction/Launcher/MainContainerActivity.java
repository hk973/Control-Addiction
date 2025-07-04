package com.genzopia.addiction.Launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.genzopia.addiction.R;
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            CounterManager cm = new CounterManager();
            int counte = cm.increment(MainContainerActivity.this);
            boolean reviewShown = cm.getReview(MainContainerActivity.this);
            Log.e("test555", String.valueOf(counte));
            runOnUiThread(() -> handleCounterResult(counte, reviewShown));
        });
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
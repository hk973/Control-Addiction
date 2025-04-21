package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

public class HomeFragment2 extends Fragment {

    private Handler timeUpdateHandler;
    private Runnable timeUpdateRunnable;
    private TextView timeTextView;
    private SharedPrefHelper sharedPrefHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activty_home_selected, container, false);
        timeTextView = view.findViewById(R.id.textView_time);
        sharedPrefHelper = new SharedPrefHelper(requireContext());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupStatusBar();
        setupShortcuts();
        setupBatteryOptimizationCheck();
        setupLifecycleObservers();
    }

    private void setupLifecycleObservers() {
        getViewLifecycleOwner().getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_RESUME) {
                startTimeUpdates();
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                stopTimeUpdates();
            }
        });
    }

    private void startTimeUpdates() {
        timeUpdateHandler = new Handler(Looper.getMainLooper());
        timeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimeDisplay();
                timeUpdateHandler.postDelayed(this, 1000);
            }
        };
        timeUpdateHandler.post(timeUpdateRunnable);
    }

    private void stopTimeUpdates() {
        if (timeUpdateHandler != null && timeUpdateRunnable != null) {
            timeUpdateHandler.removeCallbacks(timeUpdateRunnable);
        }
    }

    private void updateTimeDisplay() {
        boolean isActive = sharedPrefHelper.getTimeActivateStatus();
        long startTime = sharedPrefHelper.getStartTime();
        int initialDuration = sharedPrefHelper.getInitialDuration();

        if (isActive && startTime > 0 && initialDuration > 0) {
            long currentTime = SystemClock.elapsedRealtime();
            long elapsedSeconds = (currentTime - startTime) / 1000;
            int remainingTime = (int) (initialDuration - elapsedSeconds);

            if (remainingTime > 0) {
                timeTextView.setText(getString(R.string.time_remaining, formatTime(remainingTime)));
            } else {
                timeTextView.setText(R.string.time_expired);
                handleTimerExpiration();
            }
        } else {
            timeTextView.setText(R.string.timer_inactive);
        }
    }

    private void handleTimerExpiration() {
        // Add any expiration handling logic needed in the fragment
        stopTimeUpdates();
    }

    private void setupBatteryOptimizationCheck() {
        Context context = requireContext();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        String packageName = context.getPackageName();

        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    private void setupStatusBar() {
        Window window = requireActivity().getWindow();
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isSystemDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        int statusBarColor = isSystemDarkMode ?
                ContextCompat.getColor(requireContext(), R.color.black) :
                ContextCompat.getColor(requireContext(), R.color.white);

        window.setStatusBarColor(statusBarColor);

        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(
                window, window.getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isSystemDarkMode);
    }

    private void setupShortcuts() {
        ImageButton phoneButton = requireView().findViewById(R.id.phoneButton);
        ImageButton cameraButton = requireView().findViewById(R.id.cameraButton);

        phoneButton.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL)));
        cameraButton.setOnClickListener(v -> startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimeUpdates();
        timeTextView = null;
    }
}
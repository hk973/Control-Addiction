package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

public class HomeFragment2 extends Fragment {
    private long time;
    private long startTime;
    private long current_time;
    private Handler handler;
    private Runnable updateRunnable;
    TextView timerText;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activty_home_selected, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupStatusBar();
        setupShortcuts();
        Context context=getContext();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        String packageName = context.getPackageName();
        SharedPrefHelper sp=new SharedPrefHelper(getContext());
        time=sp.getTimeLimitValue();
        startTime = sp.getStartTime();
        timerText= getView().findViewById(R.id.textView_time);
        ImageView cameraButton = requireView().findViewById(R.id.cameraButton);
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0); // 0 = grayscale
        cameraButton.setColorFilter(new ColorMatrixColorFilter(matrix));
        SharedPrefHelper ss=new SharedPrefHelper(getContext());
        String packagename = ss.getString(context,"shortcut","");
        PackageManager pmm = context.getPackageManager();

// Get app icon and label
        try {
            ApplicationInfo appInfo = pmm.getApplicationInfo(packagename, 0);
            Drawable appIcon = appInfo.loadIcon(pmm); // Returns Drawable
            // Use in ImageView/TextView
            cameraButton.setImageDrawable(appIcon);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        startRealtimeUpdates();
    }
    @Override
    public void onPause() {
        super.onPause();
        stopRealtimeUpdates();
    }
    private void startRealtimeUpdates() {
        handler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdown();
                // Repeat every second
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
    }

    private void stopRealtimeUpdates() {
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private void updateCountdown() {
        SharedPrefHelper sp = new SharedPrefHelper(requireContext());
       long remaintime= sp.getRemainingTimeMillis();
       timerText.setText(formatTime( remaintime/1000));
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
        ImageView phoneButton = requireView().findViewById(R.id.phoneButton);
        ImageView cameraButton = requireView().findViewById(R.id.cameraButton);

        phoneButton.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL)));
        cameraButton.setOnClickListener(v -> new PopupSelectApp(getContext()).showlock(cameraButton));


    }
    public  String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
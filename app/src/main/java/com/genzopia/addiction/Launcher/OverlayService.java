package com.genzopia.addiction.Launcher;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.*;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.*;
import com.genzopia.addiction.R;

import java.util.List;

public class OverlayService extends Service {


    private WindowManager windowManager;
    private View overlayView;


    @Override
    public void onCreate() {
        super.onCreate();

        showOverlay();
    }

    private void showOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show();
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_dialog, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // Full-screen blocking parameters
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Prevent taking focus initially
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;

        windowManager.addView(overlayView, params);

        // Block back button
        overlayView.setFocusableInTouchMode(true);
        overlayView.requestFocus();
        overlayView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true; // Consume back button press
            }
            return false;
        });

        Button okButton = overlayView.findViewById(R.id.ok_button);
        okButton.setOnClickListener(v -> {
            stopSelf();
            NotificationBarDetectorService service = NotificationBarDetectorService.getInstance();
            if (service != null) {
                service.goToHomeScreen();
            } else {
                Toast.makeText(this, "AccessibilityService not running", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(this, MainContainerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });


    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


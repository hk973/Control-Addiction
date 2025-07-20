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

import com.genzopia.addiction.R;

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
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                // Do NOT include FLAG_NOT_TOUCH_MODAL or FLAG_WATCH_OUTSIDE_TOUCH
                , // <- this comma is required
                PixelFormat.TRANSLUCENT
        );


        params.gravity = Gravity.CENTER;
        params.windowAnimations = android.R.style.Animation_Toast; // optional

        windowManager.addView(overlayView, params);

        // Button logic
        Button okButton = overlayView.findViewById(R.id.ok_button);
        Button unlockButton = overlayView.findViewById(R.id.unlock_button);

        okButton.setOnClickListener(v -> {
            stopSelf();
            Intent intent = new Intent(this, MainContainerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        unlockButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), BillingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (overlayView != null && windowManager != null) {
                windowManager.removeView(overlayView);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

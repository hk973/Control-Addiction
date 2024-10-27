package com.example.addiction20;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 100;
    private static final int REQUEST_CODE_USAGE_PERMISSION = 101;

    private Button notificationPermissionButton;
    private Button usagePermissionButton;
    private Button setLauncherButton;
    private View dot1, dot2, dot3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationPermissionButton = findViewById(R.id.notificationPermissionButton);
        usagePermissionButton = findViewById(R.id.usagePermissionButton);
        setLauncherButton = findViewById(R.id.setLauncherButton);

        // Initialize dot views
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        // Initial dot setup based on granted permissions
        if (isNotificationPermissionGranted()) {
            notificationPermissionButton.setVisibility(View.GONE);
            usagePermissionButton.setVisibility(View.VISIBLE);
            setDotIndicator(2); // Move dot to second position
        } else {
            setDotIndicator(1); // Move dot to first position
        }

        if (isUsagePermissionGranted()) {
            usagePermissionButton.setVisibility(View.GONE);
            setLauncherButton.setVisibility(View.VISIBLE);
            setDotIndicator(3); // Move dot to third position
        }

        notificationPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestNotificationPermission();
            }
        });

        usagePermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestUsagePermission();
            }
        });

        setLauncherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestHomeLauncherPermission();
            }
        });
    }

    private void setDotIndicator(int step) {
        dot1.setBackgroundResource(step == 1 ? R.drawable.dot_selected : R.drawable.dot_unselected);
        dot2.setBackgroundResource(step == 2 ? R.drawable.dot_selected : R.drawable.dot_unselected);
        dot3.setBackgroundResource(step == 3 ? R.drawable.dot_selected : R.drawable.dot_unselected);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 and above
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATION_PERMISSION);
            } else {
                // Permission already granted
                usagePermissionButton.setVisibility(View.VISIBLE);
                notificationPermissionButton.setVisibility(View.GONE);
                setDotIndicator(2);
            }
        } else {
            // For older versions, skip the notification permission
            usagePermissionButton.setVisibility(View.VISIBLE);
            notificationPermissionButton.setVisibility(View.GONE);
            setDotIndicator(2);
        }
    }

    private void requestUsagePermission() {
        if (!isUsagePermissionGranted()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_USAGE_PERMISSION);
        } else {
            setLauncherButton.setVisibility(View.VISIBLE);
            usagePermissionButton.setVisibility(View.GONE);
            setDotIndicator(3);
        }
    }

    private void requestHomeLauncherPermission() {
        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
        startActivity(intent);
        finishAffinity();
    }

    private boolean isNotificationPermissionGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isUsagePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_NOTIFICATION_PERMISSION:
                if (isNotificationPermissionGranted()) {
                    usagePermissionButton.setVisibility(View.VISIBLE);
                    notificationPermissionButton.setVisibility(View.GONE);
                    setDotIndicator(2);
                }
                break;
            case REQUEST_CODE_USAGE_PERMISSION:
                if (isUsagePermissionGranted()) {
                    setLauncherButton.setVisibility(View.VISIBLE);
                    usagePermissionButton.setVisibility(View.GONE);
                    setDotIndicator(3);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                usagePermissionButton.setVisibility(View.VISIBLE);
                notificationPermissionButton.setVisibility(View.GONE);
                setDotIndicator(2);
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

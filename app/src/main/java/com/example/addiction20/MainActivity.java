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
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
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
    private Button proceedTermsButton;
    private Button setLauncherButton;
    private View dot1, dot2, dot3, dot4;
    WebView webView;
    private CheckBox termsCheckBox;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView= findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true); // Enable JavaScript if needed
        webView.loadUrl("file:///android_asset/tandc.html"); // Load the existing HTML file from assets // Load the HTML file from asset

        notificationPermissionButton = findViewById(R.id.notificationPermissionButton);
        usagePermissionButton = findViewById(R.id.usagePermissionButton);
        proceedTermsButton = findViewById(R.id.proceedButton);
        setLauncherButton = findViewById(R.id.setLauncherButton);

        // Initialize dot views
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);

        // Initialize terms section views

        termsCheckBox = findViewById(R.id.termsCheckbox);

        // Initial dot setup based on granted permissions
        if (isNotificationPermissionGranted()) {
            notificationPermissionButton.setVisibility(View.GONE);
            usagePermissionButton.setVisibility(View.VISIBLE);
            setDotIndicator(2);
        } else {
            setDotIndicator(1);
        }

        if (isUsagePermissionGranted()) {
            usagePermissionButton.setVisibility(View.GONE);
            termsCheckBox.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);
            setDotIndicator(3);
        }

        // Listeners for permission buttons
        notificationPermissionButton.setOnClickListener(v -> requestNotificationPermission());

        usagePermissionButton.setOnClickListener(v -> requestUsagePermission());

        // Listener for Terms and Conditions checkbox
        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                proceedTermsButton.setVisibility(View.VISIBLE);
            } else {
                proceedTermsButton.setVisibility(View.GONE);
            }
        });

        // Listener for proceed button
        proceedTermsButton.setOnClickListener(v -> {
            // Show the set launcher button and hide terms-related UI elements
            setLauncherButton.setVisibility(View.VISIBLE);
            proceedTermsButton.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            termsCheckBox.setVisibility(View.GONE);

            // Update dot indicator to step 4
            setDotIndicator(4);
        });


        setLauncherButton.setOnClickListener(v -> requestHomeLauncherPermission());
    }

    private void setDotIndicator(int step) {
        dot1.setBackgroundResource(step == 1 ? R.drawable.dot_selected : R.drawable.dot_unselected);
        dot2.setBackgroundResource(step == 2 ? R.drawable.dot_selected : R.drawable.dot_unselected);
        dot3.setBackgroundResource(step == 3 ? R.drawable.dot_selected : R.drawable.dot_unselected);
        dot4.setBackgroundResource(step == 4 ? R.drawable.dot_selected : R.drawable.dot_unselected);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATION_PERMISSION);
            } else {
                usagePermissionButton.setVisibility(View.VISIBLE);
                notificationPermissionButton.setVisibility(View.GONE);
                setDotIndicator(2);
            }
        } else {
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

            termsCheckBox.setVisibility(View.VISIBLE);
            usagePermissionButton.setVisibility(View.GONE);
            termsCheckBox.setVisibility(View.GONE);
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
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION && isNotificationPermissionGranted()) {
            usagePermissionButton.setVisibility(View.VISIBLE);
            notificationPermissionButton.setVisibility(View.GONE);
            setDotIndicator(2);
        } else if (requestCode == REQUEST_CODE_USAGE_PERMISSION && isUsagePermissionGranted()) {

            webView.setVisibility(View.VISIBLE);
            termsCheckBox.setVisibility(View.VISIBLE);
            usagePermissionButton.setVisibility(View.GONE);
            setDotIndicator(3);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            usagePermissionButton.setVisibility(View.VISIBLE);
            notificationPermissionButton.setVisibility(View.GONE);
            setDotIndicator(2);
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}

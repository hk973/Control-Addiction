// UsagePermissionFragment.java
package com.genzopia.addiction.permission;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.genzopia.addiction.R;

public class UsagePermissionFragment extends BasePermissionFragment {

    private static final int REQUEST_CODE_USAGE_PERMISSION = 101;
    private Button requestPermissionButton;
    private ImageView statusImage;
    private TextView statusText;

    public static UsagePermissionFragment newInstance() {
        return new UsagePermissionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission, container, false);

        // Initialize views
        TextView titleText = view.findViewById(R.id.permissionTitle);
        TextView descriptionText = view.findViewById(R.id.permissionDescription);
        requestPermissionButton = view.findViewById(R.id.requestPermissionButton);
        statusImage = view.findViewById(R.id.statusImage);
        statusText = view.findViewById(R.id.statusText);

        // Set content
        titleText.setText("Usage Access Permission");
        descriptionText.setText("We need usage access permission to monitor how much time you spend on different apps. This helps us provide you with insights about your screen time habits and help you reduce addiction.");
        requestPermissionButton.setText("Grant Usage Access Permission");

        // Set position
        position = 1;

        // Set click listener
        requestPermissionButton.setOnClickListener(v -> requestUsagePermission());

        return view;
    }

    private void requestUsagePermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE_USAGE_PERMISSION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_USAGE_PERMISSION) {
            checkPermissionStatus();
        }
    }

    @Override
    protected void checkPermissionStatus() {
        if (isPermissionGranted()) {
            updatePermissionGrantedUI();
        } else {
            updatePermissionNotGrantedUI();
        }
    }

    @Override
    public boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager appOpsManager = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        }
        return true;
    }

    private void updatePermissionGrantedUI() {
        statusImage.setImageResource(R.drawable.ic_check_circle);
        statusText.setText("Permission Granted");
        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
        requestPermissionButton.setVisibility(View.GONE);

        notifyPermissionGranted();
    }

    private void updatePermissionNotGrantedUI() {
        statusImage.setImageResource(R.drawable.ic_pending);
        statusText.setText("Permission Required");
        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
        requestPermissionButton.setVisibility(View.VISIBLE);
    }
}
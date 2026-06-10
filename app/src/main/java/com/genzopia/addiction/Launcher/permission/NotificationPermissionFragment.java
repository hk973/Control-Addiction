package com.genzopia.addiction.Launcher.permission;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.genzopia.addiction.Launcher.SharedPrefHelper;
import com.genzopia.addiction.R;

/**
 * Onboarding step that requests the POST_NOTIFICATIONS runtime permission
 * on Android 13+ (API 33+). On older devices the permission is not required
 * and this screen auto-advances immediately.
 */
public class NotificationPermissionFragment extends BasePermissionFragment {

    private ImageView statusImage;
    private TextView statusText;
    private Button requestPermissionButton;

    // Modern permission launcher — avoids deprecated onRequestPermissionsResult
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        new SharedPrefHelper(requireContext())
                                .setNotifPermissionGranted(requireContext(), granted);
                        checkPermissionStatus();
                    });

    public static NotificationPermissionFragment newInstance() {
        return new NotificationPermissionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission, container, false);

        TextView titleText       = view.findViewById(R.id.permissionTitle);
        TextView descriptionText = view.findViewById(R.id.permissionDescription);
        requestPermissionButton  = view.findViewById(R.id.requestPermissionButton);
        statusImage              = view.findViewById(R.id.statusImage);
        statusText               = view.findViewById(R.id.statusText);

        titleText.setText("Notification Permission");
        descriptionText.setText(
                "Allow notifications so we can send you helpful reminders, " +
                "challenges, and important updates directly to your device.");
        requestPermissionButton.setText("Allow Notifications");

        // Position 0 — first page of the onboarding pager
        position = 0;

        requestPermissionButton.setOnClickListener(v -> requestPermission());

        return view;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // Below API 33 notifications are granted implicitly — auto-advance
            notifyPermissionGranted();
        }
    }

    @Override
    protected void checkPermissionStatus() {
        if (isPermissionGranted()) {
            statusImage.setImageResource(R.drawable.ic_check_circle);
            statusText.setText("Permission Granted");
            statusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.green));
            requestPermissionButton.setVisibility(View.GONE);
            notifyPermissionGranted();
        } else {
            statusImage.setImageResource(R.drawable.ic_pending);
            statusText.setText("Permission Required");
            statusText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.red));
            requestPermissionButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // Implicitly granted on API < 33
        return true;
    }
}

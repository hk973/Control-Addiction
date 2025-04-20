package com.genzopia.addiction.permission;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.genzopia.addiction.R;

public class NotificationPermissionFragment extends BasePermissionFragment {

    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 100;
    private Button requestPermissionButton;
    private ImageView statusImage;
    private TextView statusText;

    public static NotificationPermissionFragment newInstance() {
        return new NotificationPermissionFragment();
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
        titleText.setText("Notification Permission");
        descriptionText.setText("We need notification permission to alert you when you've reached your screen time limits and to provide timely reminders to help you reduce your screen time.");
        requestPermissionButton.setText("Grant Notification Permission");

        // Set position
        position = 0;

        // Set click listener
        requestPermissionButton.setOnClickListener(v -> requestNotificationPermission());

        return view;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATION_PERMISSION);
            } else {
                updatePermissionGrantedUI();
            }
        } else {
            // For versions before Android 13, notification permission is granted by default
            updatePermissionGrantedUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updatePermissionGrantedUI();
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
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
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

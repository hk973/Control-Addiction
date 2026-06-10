package com.genzopia.addiction.Launcher.permission;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.genzopia.addiction.R;

public class OverlayPermissionFragment extends BasePermissionFragment {

    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 101;
    private Button requestPermissionButton;
    private ImageView statusImage;
    private TextView statusText;

    public static OverlayPermissionFragment newInstance() {
        return new OverlayPermissionFragment();
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
        titleText.setText("Overlay Permission");
        descriptionText.setText("We need overlay permission to display time limit warnings and app blocking screens over other apps. This helps you stay mindful of your screen time usage.");
        requestPermissionButton.setText("Grant Permission");

        // Set fragment position for navigation
        position = 1;

        // Update UI based on current status
        checkPermissionStatus();

        // Show disclosure dialog before navigating to Settings
        requestPermissionButton.setOnClickListener(v -> showDisclosureDialog());

        return view;
    }

    private void showDisclosureDialog() {
        // Inflate custom layout with checkbox
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.accessibility_consent_dialog, null);

        CheckBox consentCheckbox = dialogView.findViewById(R.id.consentCheckbox);
        TextView messageText = dialogView.findViewById(R.id.messageText);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Overlay Permission Required")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Allow", null) // Set to null to override default dismissal
                .setNegativeButton("Deny", null)
                .create();

        // Custom message with explicit details
        messageText.setText("To help you manage screen time effectively, this app needs Overlay permission to:\n\n" +
                "• Display time limit warnings over other apps\n" +
                "• Show app blocking screens when limits are reached\n" +
                "• Provide visual reminders about your usage\n" +
                "• Help you stay mindful of your screen time\n\n" +
                "By checking the box below, you confirm you understand these functions. No personal data will be collected or shared.");

        dialog.setOnShowListener(dialogInterface -> {
            Button allowButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button denyButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            allowButton.setEnabled(false); // Initially disabled

            consentCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Only enable Allow button when checkbox is checked
                allowButton.setEnabled(isChecked);
            });
            
            denyButton.setOnClickListener(view -> {
                dialog.dismiss();
            });

            allowButton.setOnClickListener(v -> {
                if (consentCheckbox.isChecked()) {
                    dialog.dismiss();
                    openOverlaySettings();
                }
            });
        });

        dialog.show();
    }

    private void openOverlaySettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        }
        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(requireContext());
        } else {
            // For older versions, assume permission is granted
            return true;
        }
    }

    private void updatePermissionGrantedUI() {
        statusImage.setImageResource(R.drawable.ic_check_circle);
        statusText.setText("Permission Granted");
        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
        requestPermissionButton.setVisibility(View.GONE);

        // Notify host activity
        notifyPermissionGranted();
    }

    private void updatePermissionNotGrantedUI() {
        statusImage.setImageResource(R.drawable.ic_pending);
        statusText.setText("Permission Required");
        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
        requestPermissionButton.setVisibility(View.VISIBLE);
    }
} 
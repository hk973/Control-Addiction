package com.genzopia.addiction.permission;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
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

import com.genzopia.addiction.NotificationBarDetectorService;
import com.genzopia.addiction.R;

public class AccessibilityPermissionFragment extends BasePermissionFragment {

    private static final int REQUEST_CODE_ACCESSIBILITY_PERMISSION = 102;
    private Button requestPermissionButton;
    private ImageView statusImage;
    private TextView statusText;

    public static AccessibilityPermissionFragment newInstance() {
        return new AccessibilityPermissionFragment();
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
        titleText.setText("Accessibility Permission");
        descriptionText.setText("We use Accessibility service to monitor and block apps when you've exceeded your time limits. No personal data is collected. Please grant permission to continue.");
        requestPermissionButton.setText("Grant Permission");

        // Set fragment position for navigation
        position = 0;

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
                .setTitle("Accessibility Permission Required")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Allow", null) // Set to null to override default dismissal
                .setNegativeButton("Deny", null)
                .create();

        // Custom message with explicit details
        messageText.setText("To help you manage screen time and digital wellbeing, this app needs Accessibility permission to:\n\n" +
                "• Monitor app usage\n" +
                "• Lock specific apps when time limits are reached\n" +
                "• Enforce screen time controls\n\n" +
                "By checking the box below, you confirm you understand these functions. No personal data will be collected or shared.");

        dialog.setOnShowListener(dialogInterface -> {
            Button allowButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button denybutton=dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            allowButton.setEnabled(false); // Initially disabled

            consentCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Only enable Allow button when checkbox is checked
                allowButton.setEnabled(isChecked);
            });
            denybutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            allowButton.setOnClickListener(v -> {
                if (consentCheckbox.isChecked()) {
                    dialog.dismiss();
                    openAccessibilitySettings();
                }
            });
        });

        dialog.show();
    }
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE_ACCESSIBILITY_PERMISSION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ACCESSIBILITY_PERMISSION) {
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
        String serviceId = getContext().getPackageName() + "/" + NotificationBarDetectorService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
                getContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices == null) return false;

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            if (splitter.next().equalsIgnoreCase(serviceId)) {
                return true;
            }
        }
        return false;
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

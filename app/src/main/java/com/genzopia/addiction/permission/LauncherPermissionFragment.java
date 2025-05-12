package com.genzopia.addiction.permission;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.genzopia.addiction.MainContainerActivity;
import com.genzopia.addiction.R;

public class LauncherPermissionFragment extends BasePermissionFragment {

    private Button setLauncherButton;

    private ImageView statusImage;
    private TextView statusText;
    private Button button;

    public static LauncherPermissionFragment newInstance() {
        return new LauncherPermissionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_launcher, container, false);

        // Initialize views
        TextView titleText = view.findViewById(R.id.launcherTitle);
        TextView descriptionText = view.findViewById(R.id.launcherDescription);
        setLauncherButton = view.findViewById(R.id.setLauncherButton);

        statusImage = view.findViewById(R.id.statusImage);
        statusText = view.findViewById(R.id.statusText);
        button=view.findViewById(R.id.proceedButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isMyAppDefaultLauncher()) {
                    // Fire your intent (example: launch MainActivity)
                    Intent intent = new Intent(getContext(), MainContainerActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                } else {
                    // Show toast
                    Toast.makeText(getContext(), "Please set this app as the default launcher", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set content
        titleText.setText("Set as Default Launcher");
        descriptionText.setText("For the best experience, set our app as your default launcher. This will help you control your phone usage by giving you a more mindful home screen experience.");

        // Set position
        position = 2;

        // Set click listeners
        setLauncherButton.setOnClickListener(v -> requestHomeLauncherPermission());


        return view;
    }

    private void requestHomeLauncherPermission() {
        Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
        startActivity(intent);
    }
    private boolean isMyAppDefaultLauncher() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = requireContext().getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String currentLauncherPackage = resolveInfo.activityInfo.packageName;

        return currentLauncherPackage.equals(requireContext().getPackageName());
    }

    @Override
    protected void checkPermissionStatus() {
        // We don't have a clear way to check if our app is set as the default launcher
        // So we'll just provide the option and let the user finish when ready
        updateUI();
    }

    @Override
    public boolean isPermissionGranted() {
        return true;
    }



    private void updateUI() {
        statusImage.setImageResource(R.drawable.ic_info);
        statusText.setText("We will fight with addiction together");
        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
        setLauncherButton.setVisibility(View.VISIBLE);

    }
}

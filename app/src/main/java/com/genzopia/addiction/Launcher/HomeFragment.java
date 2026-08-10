package com.genzopia.addiction.Launcher;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.genzopia.addiction.R;

public class HomeFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupStatusBar();
        setupShortcuts();
        showShortcutIcon(view);
    }

    /** Paints the shortcut button with the icon of the app the user assigned, if any. */
    private void showShortcutIcon(View view) {
        Context context = getContext();
        if (context == null) return;

        ImageView shortcutButton = view.findViewById(R.id.cameraButton);
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0); // 0 = grayscale
        shortcutButton.setColorFilter(new ColorMatrixColorFilter(matrix));

        String shortcutPackage = new SharedPrefHelper(context).getString(context, "shortcut", "");
        if (TextUtils.isEmpty(shortcutPackage)) return;

        try {
            PackageManager pm = context.getPackageManager();
            Drawable icon = pm.getApplicationInfo(shortcutPackage, 0).loadIcon(pm);
            shortcutButton.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            // The shortcut target was uninstalled — keep the default icon.
            Log.d("HomeFragment", "Shortcut app not installed: " + shortcutPackage);
        }
    }


    private void setupStatusBar() {
        Window window = requireActivity().getWindow();
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isSystemDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        int statusBarColor = isSystemDarkMode ?
                ContextCompat.getColor(requireContext(), R.color.black) :
                ContextCompat.getColor(requireContext(), R.color.white);

        window.setStatusBarColor(statusBarColor);

        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(
                window, window.getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isSystemDarkMode);
    }

    private void setupShortcuts() {
        ImageView phoneButton = requireView().findViewById(R.id.phoneButton);
        ImageView cameraButton = requireView().findViewById(R.id.cameraButton);

        phoneButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL));
            } catch (ActivityNotFoundException e) {
                // Tablets and some ROMs have no dialer at all.
                Toast.makeText(requireContext(), "No dialer app found", Toast.LENGTH_SHORT).show();
            }
        });
        cameraButton.setOnClickListener(v -> {
            if (isAdded()) new PopupSelectApp(requireContext()).show(cameraButton);
        });
        cameraButton.setOnLongClickListener(view -> {
            if (isAdded()) new PopupSelectApp(requireContext()).show2(cameraButton);
            return true;
        });
    }
}
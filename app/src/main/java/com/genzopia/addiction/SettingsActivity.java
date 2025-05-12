package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowInsetsControllerCompat;

public class SettingsActivity extends BaseActivity {
    private SharedPrefHelper sharedPrefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPrefHelper = new SharedPrefHelper(this);

        // Apply grayscale effect if Gray Mode is enabled
        applyGrayScaleIfNeeded();

        // Set the system status bar color based on the current theme
        Window window = getWindow();
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isSystemDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        int statusBarColor = isSystemDarkMode ? getResources().getColor(R.color.black, getTheme())
                : getResources().getColor(R.color.white, getTheme());

        window.setStatusBarColor(statusBarColor);

        // Change status bar icon color based on the system theme
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isSystemDarkMode);

        // Setting up views for open method and theme options
        ImageView backButton = findViewById(R.id.backButton);
        RadioGroup openMethodGroup = findViewById(R.id.openMethodGroup);
        RadioButton clickOption = findViewById(R.id.clickOption);
        RadioButton holdOption = findViewById(R.id.holdOption);

        // Theme selection views
        RadioGroup themeSelectionGroup = findViewById(R.id.themeselection_grp);
        RadioButton darkModeOption = findViewById(R.id.darkmode_opt);
        RadioButton lightModeOption = findViewById(R.id.lightmode_opt);
        RadioButton grayModeOption = findViewById(R.id.gray_opt);

        // Set current open method selection based on saved preferences
        boolean isClickToOpen = sharedPrefHelper.isClickToOpen();
        clickOption.setChecked(isClickToOpen);
        holdOption.setChecked(!isClickToOpen);

        // Set current theme selection based on saved preferences
        if (sharedPrefHelper.isDarkModeEnabled()) {
            darkModeOption.setChecked(true);
        } else if (sharedPrefHelper.isGrayModeEnabled()) {
            grayModeOption.setChecked(true);
        } else {
            lightModeOption.setChecked(true);
        }

        // Back button behavior
        backButton.setOnClickListener(v -> finish());

        // Handle open method selection (Click or Hold)
        openMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isClick = checkedId == R.id.clickOption;
            sharedPrefHelper.setClickToOpen(isClick);
        });

        // Handle theme selection changes
        themeSelectionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.darkmode_opt) {
                sharedPrefHelper.setDarkModeEnabled(true);
                sharedPrefHelper.setGrayModeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (checkedId == R.id.lightmode_opt) {
                sharedPrefHelper.setDarkModeEnabled(false);
                sharedPrefHelper.setGrayModeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.gray_opt) {
                sharedPrefHelper.setDarkModeEnabled(false);
                sharedPrefHelper.setGrayModeEnabled(true);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            recreate(); // Recreate activity to apply new theme/filter
        });
    }

    // Apply grayscale effect to the root view if Gray Mode is enabled
    private void applyGrayScaleIfNeeded() {
        if (sharedPrefHelper != null && sharedPrefHelper.isGrayModeEnabled()) {
            // Get the root view of the activity
            View root = getWindow().getDecorView();
            root.setLayerType(View.LAYER_TYPE_HARDWARE, null); // Enable hardware layer for better performance

            // Create a ColorMatrix for grayscale effect
            Paint paint = new Paint();
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); // Set saturation to 0 for grayscale
            paint.setColorFilter(new ColorMatrixColorFilter(matrix));

            // Apply the color filter to the root view
            root.setLayerPaint(paint);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

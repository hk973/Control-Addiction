package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

public class SettingsActivity extends AppCompatActivity {
    private SharedPrefHelper sharedPrefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Window window = getWindow();
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isSystemDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        int statusBarColor = isSystemDarkMode ? getResources().getColor(R.color.black, getTheme())
                : getResources().getColor(R.color.white, getTheme());

        window.setStatusBarColor(statusBarColor);

        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isSystemDarkMode);

        sharedPrefHelper = new SharedPrefHelper(this);

        ImageView backButton = findViewById(R.id.backButton);
        RadioGroup openMethodGroup = findViewById(R.id.openMethodGroup);
        RadioButton clickOption = findViewById(R.id.clickOption);
        RadioButton holdOption = findViewById(R.id.holdOption);

        // Set current selection
        boolean isClickToOpen = sharedPrefHelper.isClickToOpen();
        clickOption.setChecked(isClickToOpen);
        holdOption.setChecked(!isClickToOpen);

        backButton.setOnClickListener(v -> finish());

        openMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isClick = checkedId == R.id.clickOption;
            sharedPrefHelper.setClickToOpen(isClick);
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }
}

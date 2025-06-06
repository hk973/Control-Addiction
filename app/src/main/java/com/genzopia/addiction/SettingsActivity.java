package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

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
        RadioButton systemOption = findViewById(R.id.grayd_opt);
        TextView dropdownHeader = findViewById(R.id.dropdown_header);
        LinearLayout dropdownContent = findViewById(R.id.dropdown_content);
        // Initialize the question and answer views
        TextView faqQuestion1 = findViewById(R.id.faq_question_1);
        TextView faqAnswer1 = findViewById(R.id.faq_answer_1);

        TextView faqQuestion2 = findViewById(R.id.faq_question_2);
        TextView faqAnswer2 = findViewById(R.id.faq_answer_2);

        TextView faqQuestion3 = findViewById(R.id.faq_question_3);
        TextView faqAnswer3 = findViewById(R.id.faq_answer_3);

        TextView faqQuestion4 = findViewById(R.id.faq_question_4);
        TextView faqAnswer4 = findViewById(R.id.faq_answer_4);

        TextView faqQuestion5 = findViewById(R.id.faq_question_5);
        TextView faqAnswer5 = findViewById(R.id.faq_answer_5);

// Set the initial visibility to gone
        faqAnswer1.setVisibility(View.GONE);
        faqAnswer2.setVisibility(View.GONE);
        faqAnswer3.setVisibility(View.GONE);
        faqAnswer4.setVisibility(View.GONE);
        faqAnswer5.setVisibility(View.GONE);

// Set OnClickListeners for each question
        faqQuestion1.setOnClickListener(v -> {
            if (faqAnswer1.getVisibility() == View.VISIBLE) {
                faqAnswer1.setVisibility(View.GONE);
            } else {
                faqAnswer1.setVisibility(View.VISIBLE);
            }
        });

        faqQuestion2.setOnClickListener(v -> {
            if (faqAnswer2.getVisibility() == View.VISIBLE) {
                faqAnswer2.setVisibility(View.GONE);
            } else {
                faqAnswer2.setVisibility(View.VISIBLE);
            }
        });

        faqQuestion3.setOnClickListener(v -> {
            if (faqAnswer3.getVisibility() == View.VISIBLE) {
                faqAnswer3.setVisibility(View.GONE);
            } else {
                faqAnswer3.setVisibility(View.VISIBLE);
            }
        });

        faqQuestion4.setOnClickListener(v -> {
            if (faqAnswer4.getVisibility() == View.VISIBLE) {
                faqAnswer4.setVisibility(View.GONE);
            } else {
                faqAnswer4.setVisibility(View.VISIBLE);
            }
        });

        faqQuestion5.setOnClickListener(v -> {
            if (faqAnswer5.getVisibility() == View.VISIBLE) {
                faqAnswer5.setVisibility(View.GONE);
            } else {
                faqAnswer5.setVisibility(View.VISIBLE);
            }
        });




        // Toggle visibility when header is clicked
        dropdownHeader.setOnClickListener(v -> {
            if (dropdownContent.getVisibility() == View.VISIBLE) {
                dropdownContent.setVisibility(View.GONE);
            } else {
                dropdownContent.setVisibility(View.VISIBLE);
            }
        });



        // Set current open method selection based on saved preferences
        boolean isClickToOpen = sharedPrefHelper.isClickToOpen();
        clickOption.setChecked(isClickToOpen);
        holdOption.setChecked(!isClickToOpen);

        // Set current theme selection based on saved preferences
        if (sharedPrefHelper.isDarkModeEnabled()) {
            darkModeOption.setChecked(true);
            grayModeOption.setChecked(false);
            lightModeOption.setChecked(false);
            systemOption.setChecked(false);
        } else if (sharedPrefHelper.isGrayModeEnabled()) {
            grayModeOption.setChecked(true);
            darkModeOption.setChecked(false);
            lightModeOption.setChecked(false);
            systemOption.setChecked(false);
            Log.d("tsss","grey");
        } else if (sharedPrefHelper.isFollowSystemThemeEnabled()){
            lightModeOption.setChecked(false);
            grayModeOption.setChecked(false);
            darkModeOption.setChecked(false);
            systemOption.setChecked(true);
            Log.d("tsss","system");
        }else {
            lightModeOption.setChecked(true);
            grayModeOption.setChecked(false);
            darkModeOption.setChecked(false);
            systemOption.setChecked(false);
            Log.d("tsss","light");
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
                // Enable Dark Mode
                sharedPrefHelper.setDarkModeEnabled(false);
                sharedPrefHelper.setGrayModeEnabled(false);
                sharedPrefHelper.setFollowSystemThemeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                applyGrayScaleIfNeeded();
                sharedPrefHelper.setDarkModeEnabled(true);
                sharedPrefHelper.setGrayModeEnabled(false);
                sharedPrefHelper.setFollowSystemThemeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);


            } else if (checkedId == R.id.lightmode_opt) {
                // Disable both Dark Mode and Gray Mode (Light Mode)
                sharedPrefHelper.setDarkModeEnabled(false);
                sharedPrefHelper.setGrayModeEnabled(false);
                sharedPrefHelper.setFollowSystemThemeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);


            } else if (checkedId == R.id.gray_opt) {
                // Enable Gray Mode along with Dark Mode
                sharedPrefHelper.setDarkModeEnabled(false);
                sharedPrefHelper.setGrayModeEnabled(false);
                sharedPrefHelper.setFollowSystemThemeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                applyGrayScaleIfNeeded();
                sharedPrefHelper.setDarkModeEnabled(false);
                sharedPrefHelper.setGrayModeEnabled(true);
                sharedPrefHelper.setFollowSystemThemeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                applyGrayScaleIfNeeded();

            } else if (checkedId == R.id.grayd_opt) {
                // Follow system theme
                sharedPrefHelper.setDarkModeEnabled(false);
                sharedPrefHelper.setGrayModeEnabled(false);
                sharedPrefHelper.setFollowSystemThemeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                applyGrayScaleIfNeeded();
                sharedPrefHelper.setFollowSystemThemeEnabled(true);
                sharedPrefHelper.setDarkModeEnabled(false);
                sharedPrefHelper.setGrayModeEnabled(false);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

            }
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
        finish();
    }
}

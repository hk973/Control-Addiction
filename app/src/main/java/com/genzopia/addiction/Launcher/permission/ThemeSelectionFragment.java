package com.genzopia.addiction.Launcher.permission;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.genzopia.addiction.R;
import com.genzopia.addiction.Launcher.SharedPrefHelper;

public class ThemeSelectionFragment extends BasePermissionFragment {

    private CardView mobileThemeCard, greyThemeCard;
    private ImageView mobileCheck, greyCheck;
    private Button proceedButton;
    private int selectedTheme = 0; // 0=not selected, 1=mobile, 2=grey

    public static ThemeSelectionFragment newInstance() {
        return new ThemeSelectionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_theme_selection, container, false);

        // Initialize views
        TextView titleText = view.findViewById(R.id.themeTitle);
        mobileThemeCard = view.findViewById(R.id.mobileThemeCard);
        greyThemeCard = view.findViewById(R.id.greyThemeCard);
        mobileCheck = view.findViewById(R.id.mobileCheck);
        greyCheck = view.findViewById(R.id.greyCheck);
        proceedButton = view.findViewById(R.id.proceedButton);

        // Set position
        position = 2;
        proceedButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_500));
        proceedButton.setEnabled(false);
        proceedButton.setAlpha(0.5f);

        // Set click listeners
        mobileThemeCard.setOnClickListener(v -> selectTheme(1));
        greyThemeCard.setOnClickListener(v -> selectTheme(2));

        proceedButton.setOnClickListener(v -> {
            if(selectedTheme != 0) {
                applyTheme();
                notifyPermissionGranted();
            }
        });


        return view;
    }

    private void selectTheme(int theme) {
        selectedTheme = theme;
        mobileCheck.setVisibility(theme == 1 ? View.VISIBLE : View.GONE);
        greyCheck.setVisibility(theme == 2 ? View.VISIBLE : View.GONE);

        // Update button state
        proceedButton.setEnabled(true);
        proceedButton.setAlpha(1f); // Make it fully visible
        proceedButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_500));

        // Update card backgrounds
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight);
        int defaultColor = ContextCompat.getColor(requireContext(), R.color.cardBackground);

        mobileThemeCard.setCardBackgroundColor(theme == 1 ? selectedColor : defaultColor);
        greyThemeCard.setCardBackgroundColor(theme == 2 ? selectedColor : defaultColor);

    }

    private void applyTheme() {
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(getContext());


        // Call your existing theme functions here
        if(selectedTheme == 1) {
            sharedPrefHelper.setFollowSystemThemeEnabled(true);
            sharedPrefHelper.setDarkModeEnabled(false);
            sharedPrefHelper.setGrayModeEnabled(false);

        } else {
            sharedPrefHelper.setDarkModeEnabled(false);
            sharedPrefHelper.setGrayModeEnabled(true);
            sharedPrefHelper.setFollowSystemThemeEnabled(false);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        }
    }

    @Override
    protected void checkPermissionStatus() {
        if(isPermissionGranted()) {
            updatePermissionGrantedUI();
        }
    }

    @Override
    public boolean isPermissionGranted() {
        return selectedTheme != 0;
    }

    private void updatePermissionGrantedUI() {
        proceedButton.setVisibility(View.GONE);
        mobileThemeCard.setEnabled(false);
        greyThemeCard.setEnabled(false);
        notifyPermissionGranted();
    }
    // Apply grayscale effect to the root view if Gray Mode is enabled
    private void applyGrayScaleIfNeeded() {
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(requireContext());
        if (sharedPrefHelper != null && sharedPrefHelper.isGrayModeEnabled()) {
            // Get the root view of the activity's window
            Window window = requireActivity().getWindow();
            View root = window.getDecorView();
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(requireContext());
        Log.e("test234",sharedPrefHelper.isFollowSystemThemeEnabled()+","+sharedPrefHelper.isGrayModeEnabled());
        if(sharedPrefHelper.isFollowSystemThemeEnabled()||sharedPrefHelper.isGrayModeEnabled()){
            notifyPermissionGranted();
        }

    }
}
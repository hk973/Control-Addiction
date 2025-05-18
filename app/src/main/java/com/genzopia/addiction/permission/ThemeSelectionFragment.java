package com.genzopia.addiction.permission;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.genzopia.addiction.R;
import com.genzopia.addiction.SharedPrefHelper;

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
        SharedPrefHelper sp = new SharedPrefHelper(requireContext());


        // Call your existing theme functions here
        if(selectedTheme == 1) {
            // applyMobileTheme();
        } else {
            // applyGreyTheme();
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
}
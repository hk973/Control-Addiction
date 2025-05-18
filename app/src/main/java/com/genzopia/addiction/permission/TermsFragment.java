// TermsFragment.java
package com.genzopia.addiction.permission;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.genzopia.addiction.R;
import com.genzopia.addiction.SharedPrefHelper;

public class TermsFragment extends BasePermissionFragment {

    private WebView webView;
    private CheckBox termsCheckBox;
    private Button proceedButton;
    private ImageView statusImage;
    private TextView statusText;
    private boolean termsAccepted = false;

    public static TermsFragment newInstance() {
        return new TermsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terms, container, false);

        // Initialize views
        TextView titleText = view.findViewById(R.id.termsTitle);
        webView = view.findViewById(R.id.webView);
        termsCheckBox = view.findViewById(R.id.termsCheckbox);
        proceedButton = view.findViewById(R.id.proceedButton);
        statusImage = view.findViewById(R.id.statusImage);
        statusText = view.findViewById(R.id.statusText);

        // Set content
        titleText.setText("Terms and Conditions");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/tandc.html");

        // Set position
        position = 1;

        // Set listeners
        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            proceedButton.setEnabled(isChecked);
        });
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(requireContext());
        termsAccepted = sharedPrefHelper.isTermsAccepted();

        if (termsAccepted) {
            proceedButton.setEnabled(true);
            updatePermissionGrantedUI();
        }

        proceedButton.setOnClickListener(v -> {
            termsAccepted = true;
            // Save to shared preferences
            sharedPrefHelper.setTermsAccepted(true);
            updatePermissionGrantedUI();
        });

        // Initially disable the proceed button
        proceedButton.setEnabled(false);

        return view;
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
        return termsAccepted;
    }

    private void updatePermissionGrantedUI() {
        statusImage.setImageResource(R.drawable.ic_check_circle);
        statusText.setText("Terms Accepted");
        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
        webView.setVisibility(View.GONE);
        termsCheckBox.setVisibility(View.GONE);
        proceedButton.setVisibility(View.GONE);

        notifyPermissionGranted();
    }

    private void updatePermissionNotGrantedUI() {
        statusImage.setImageResource(R.drawable.ic_pending);
        statusText.setText("Terms Review Required");
        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
        webView.setVisibility(View.VISIBLE);
        termsCheckBox.setVisibility(View.VISIBLE);
        proceedButton.setVisibility(View.VISIBLE);
    }
}
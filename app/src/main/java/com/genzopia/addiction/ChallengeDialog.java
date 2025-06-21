package com.genzopia.addiction;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

class ChallengeDialog extends Dialog {
    private Button start;
    private Button remind;
    MainFragment hostFragment;

    public ChallengeDialog(@NonNull MainFragment fragment) {
        super(fragment.requireContext());
        this.hostFragment = fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove default title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_challenge);

        start = findViewById(R.id.btnStart);
        remind = findViewById(R.id.btnLater);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmationDialog();
            }
        });

        remind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        if (getWindow() != null) {
            // Make the dialog window itself fully transparent, then size it
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            getWindow().setLayout(
                    (int) (metrics.widthPixels * 0.9),
                    (int) (metrics.heightPixels * 0.8)
            );
        }
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("⚠ Warning");
        builder.setMessage("Are you sure you want to start the challenge? Once started, you will not be able to access other apps except the phone call.");

        builder.setPositiveButton("Start Challenge", (dialog, which) -> {
            // User confirmed, start the challenge
            startChallenge();
            dismiss(); // Close the main ChallengeDialog
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button colors if desired
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.RED);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.GRAY);
        }
    }

    private void startChallenge() {
        SharedPrefHelper sharedPrefHelper=new SharedPrefHelper(getContext());
        sharedPrefHelper.setChallengeStatus(getContext(),true);
        if (!hostFragment.isAccessibilityServiceEnabled(getContext(), NotificationBarDetectorService.class)) {
            hostFragment.showAccessibilityDialog();
        } else {
            hostFragment.launchDeviceCredentialVerification(30);
        }


    }
}

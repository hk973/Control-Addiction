package com.genzopia.addiction;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
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

    public ChallengeDialog(@NonNull Context context) {
        super(context);
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
        Context ctx = getContext();
        SharedPrefHelper sharedPrefHelper = null;

        try {
            sharedPrefHelper = new SharedPrefHelper(ctx);
        } catch (Exception e) {
            Toast.makeText(ctx, "Error initializing preferences: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
        }

        // Set 30 days challenge in seconds
        int totalSeconds = 30 * 24 * 60 * 60;

        try {
            sharedPrefHelper.saveStartTime(System.currentTimeMillis());
            sharedPrefHelper.saveInitialDuration(totalSeconds);
            ArrayList<String> selectedApps = new ArrayList<>();
            sharedPrefHelper.writeData(selectedApps, totalSeconds, true);
            sharedPrefHelper.setChallengeStatus(getContext(),true);
        } catch (Exception e) {
            Toast.makeText(ctx, "Error saving challenge state: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            // You might choose to abort starting the challenge here, or proceed with limited functionality
            return;
        }

        Toast.makeText(ctx, "30-day challenge activated!", Toast.LENGTH_LONG).show();

        // Prepare intent to launch MainContainerActivity2
        Intent intent = new Intent(ctx, MainContainerActivity2.class);

        // Attempt to start activity safely
        try {
            if (ctx instanceof Activity) {
                ((Activity) ctx).startActivity(intent);
            } else {
                // If context is not an Activity (unlikely for a dialog attached to an Activity, but safe fallback)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(ctx, "Error launching challenge activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            // Depending on app flow, you may choose to return here or continue
            return;
        }

        // Optionally finish the owner Activity if available
        try {
            if (getOwnerActivity() != null) {
                getOwnerActivity().finish();
            }
        } catch (Exception e) {
            // Log but don’t crash if finish fails
            e.printStackTrace();
        }

        Toast.makeText(ctx, "Challenge started! App access restricted.", Toast.LENGTH_SHORT).show();
    }
}

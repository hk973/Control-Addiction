package com.genzopia.addiction;

import android.app.Dialog;
import android.content.Context;
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

class ChallengeDialog extends Dialog {
    Button start;
    Button remind;

    public ChallengeDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            // make the dialog window itself fully transparent
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // then size it as before
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
            dismiss(); // Close the main dialog
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // User cancelled, do nothing
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize the buttons if needed
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null && negativeButton != null) {
            positiveButton.setTextColor(Color.RED);
            negativeButton.setTextColor(Color.GRAY);
        }
    }

    private void startChallenge() {
        // Implement your challenge starting logic here
        // This is where you would restrict access to other apps

        // For now, just show a toast message
        Toast.makeText(getContext(), "Challenge started! App access restricted.", Toast.LENGTH_SHORT).show();


    }
}
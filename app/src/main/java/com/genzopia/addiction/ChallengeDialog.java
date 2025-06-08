package com.genzopia.addiction;

import static java.security.AccessController.getContext;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

 class ChallengeDialog extends Dialog {

    public ChallengeDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_challenge);

        // Set dialog dimensions
        if (getWindow() != null) {
            getWindow().setLayout(
                    (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Setup buttons
        Button btnStart = findViewById(R.id.btnStart);
        Button btnLater = findViewById(R.id.btnLater);

        btnStart.setOnClickListener(v -> {
            // Start the challenge
            dismiss();
            // Add your challenge start logic here
        });

        btnLater.setOnClickListener(v -> {
            // Dismiss and remind later
            dismiss();
            // Add your remind later logic here
        });
    }
}

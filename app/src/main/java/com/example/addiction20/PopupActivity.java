package com.example.addiction20;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PopupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create an AlertDialog to act as a popup
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("App Usage Alert")
                .setMessage("You cannot use this app as it is not in the approved list.")
                .setCancelable(false) // Prevent dismissal by tapping outside
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // Optionally, close this activity when the user acknowledges
                    finish();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

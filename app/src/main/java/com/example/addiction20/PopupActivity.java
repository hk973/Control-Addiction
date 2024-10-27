package com.example.addiction20;

import android.content.Intent;
import android.os.Bundle;
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
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Action for "Yes" button: go to homepage
                    Intent intent = new Intent(PopupActivity.this, MainActivity2.class);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Action for "No" button: run custom() function
                    custom();
                    dialog.dismiss();
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Define the custom function
    private void custom() {
        // Add your custom code here
        Intent intent = new Intent(PopupActivity.this, addactivity.class);
        startActivity(intent);

    }
}

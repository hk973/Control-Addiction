package com.genzopia.addiction.Launcher;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.genzopia.addiction.R;
import com.google.android.material.button.MaterialButton;

public class InfoDsaDialog extends Dialog {
    MainFragment hostFragment;
    Boolean verify=true;

    public InfoDsaDialog(@NonNull MainFragment fragment) {
        super(fragment.requireContext());
        this.hostFragment = fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_dsa_config);

        TextView profileurl = findViewById(R.id.etProfileUrl);
        TextView Minuter = findViewById(R.id.etMinutesPerProblem);
        ImageView info=findViewById(R.id.infoIcon);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) MaterialButton start = findViewById(R.id.btnStartChall);
        start.setOnClickListener(v->{
            if (!hostFragment.isAccessibilityServiceEnabled(getContext(), NotificationBarDetectorService.class)) {
                Log.e("test999","accessibility"+hostFragment);
                hostFragment.showAccessibilityDialog();
            }else{
                if(verify){
                showStartDialog();
                }else{
                    Toast.makeText(getContext(),"Pls verify the profile url",Toast.LENGTH_SHORT).show();
                }
            }

        });
        info.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                    .setTitle("Info")
                    .setMessage("This is the number of Hours you want your phone to remain unlocked when one LeetCode problem is solved.")
                    .setPositiveButton("OK", null)
                    .show();
        });



        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }



    private void showStartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("DSA Challenge");
        builder.setMessage("After starting, you will only be able to use your phone after solving a Leetcode problem. Once your usage time expires, you must solve another problem or pay to unlock.");

        builder.setPositiveButton("Start Challenge", (dialog, which) -> {
            startChallenge();
            dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positiveButton != null) positiveButton.setTextColor(Color.RED);
        if (negativeButton != null) negativeButton.setTextColor(Color.GRAY);
    }
    private void startChallenge() {


    }
}

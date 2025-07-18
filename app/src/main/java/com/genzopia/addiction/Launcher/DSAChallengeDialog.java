package com.genzopia.addiction.Launcher;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.genzopia.addiction.R;

public class DSAChallengeDialog extends Dialog {
    private Button start;
    private Button remind;
    MainFragment hostFragment;

    public DSAChallengeDialog(@NonNull MainFragment fragment) {
        super(fragment.requireContext());
        this.hostFragment = fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_dsa_challenge); // Create a similar layout like dialog_challenge

        start = findViewById(R.id.btnStart);
        remind = findViewById(R.id.btnLater);

        start.setOnClickListener(view -> {
            InfoDsaDialog dsaDialog=new InfoDsaDialog(hostFragment);
            dsaDialog.show();
            dismiss();

        });

        remind.setOnClickListener(view -> dismiss());

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            getWindow().setLayout((int) (metrics.widthPixels * 0.9), (int) (metrics.heightPixels * 0.8));
        }
    }




}

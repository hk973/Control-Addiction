package com.genzopia.addiction;


import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;

public class ReviewDialog extends Dialog {
    public ReviewDialog(@NonNull Context context) {
        super(context, R.style.CustomDialogTheme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_review);
        setCancelable(false);

        ImageView imgEmotion = findViewById(R.id.img_emotion);
        TextView tvMessage = findViewById(R.id.tv_message);
        Button btnPositive = findViewById(R.id.btn_positive);
        Button btnNegative = findViewById(R.id.btn_negative);

        // Set emotional content
        imgEmotion.setImageResource(R.drawable.ic_heart);
        tvMessage.setText("We pour our heart into this app!\nCould you share some love with a 5-star review? 💖");

        btnPositive.setOnClickListener(v -> {
            openPlayStore();
            CounterManager cm=new CounterManager();
            cm.setreview(getContext(),true);
            dismiss();
        });

        btnNegative.setOnClickListener(v -> dismiss());
    }

    private void openPlayStore() {
        String packageName = getContext().getPackageName();
        try {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }
}

//how to use
//    private void showReviewDialog() {
//        new Handler().postDelayed(() -> {
//            if (!isFinishing()) {
//                ReviewDialog dialog = new ReviewDialog(this);
//                dialog.show();
//            }
//        }, 2000); // Show after 2 seconds delay
//    }
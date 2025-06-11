

package com.genzopia.addiction;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;

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

    }}

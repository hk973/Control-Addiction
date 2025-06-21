package com.genzopia.addiction.Launcher;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.genzopia.addiction.R;

public class GIFDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_gif);

        ImageView gifImageView = dialog.findViewById(R.id.gifImageView);
        TextView knowMoreText = dialog.findViewById(R.id.knowMoreText);

        // Calculate dialog width (90% of screen)
        DisplayMetrics metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int dialogWidth = (int) (metrics.widthPixels * 0.90);



        // Load GIF
        Glide.with(this)
                .asGif()
                .load(R.drawable.notif_til)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(gifImageView);

        // Handle know more click
        knowMoreText.setOnClickListener(v -> {
            // Add your "know more" action here
            startActivity(new Intent(requireContext(), PinInfoActivity.class));
        });

        // Set window properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);

            // Add elevation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setElevation(16f);
            }
        }

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Add enter animation
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setWindowAnimations(R.style.DialogAnimation);
        }
    }
}
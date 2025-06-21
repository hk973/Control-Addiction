package com.genzopia.addiction.Launcher;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private SharedPrefHelper sharedPrefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefHelper = new SharedPrefHelper(this);

        // Apply Gray Scale Effect to entire app
        if (sharedPrefHelper.isGrayModeEnabled()) {
            applyGrayScaleIfNeeded();
        }
    }

    private void applyGrayScaleIfNeeded() {
        if (sharedPrefHelper != null && sharedPrefHelper.isGrayModeEnabled()) {
            // Get the root view
            View rootView = getWindow().getDecorView();

            // Set the layer type to hardware to apply the color filter
            rootView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            // Create a Paint object to apply the grayscale filter
            Paint paint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0); // Set saturation to 0 to make the image grayscale

            // Apply the grayscale filter to the paint object
            paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

            // Apply the paint object to the root view
            rootView.setLayerPaint(paint);
        }
    }

}

package com.genzopia.addiction;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {
    private SharedPrefHelper sharedPrefHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefHelper = new SharedPrefHelper(getActivity());  // Use getActivity() to access the context

        // Apply Gray Scale Effect to entire app if the preference is enabled
        if (sharedPrefHelper.isGrayModeEnabled()) {
            applyGrayScaleIfNeeded();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the fragment's layout
        return inflater.inflate(R.layout.fragment_base, container, false);  // Replace with your actual fragment layout
    }

    private void applyGrayScaleIfNeeded() {
        if (sharedPrefHelper != null && sharedPrefHelper.isGrayModeEnabled()) {
            // Get the root view of the fragment's view hierarchy
            View rootView = getView();  // Use getView() to get the current fragment's root view

            if (rootView != null) {
                // Set the layer type to hardware to apply the color filter
                rootView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                // Create a Paint object to apply the grayscale filter
                Paint paint = new Paint();
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setSaturation(0); // Set saturation to 0 to make the view grayscale

                // Apply the grayscale filter to the paint object
                paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

                // Apply the paint object to the root view
                rootView.setLayerPaint(paint);
            }
        }
    }

    // Additional helper methods for common fragment tasks (e.g., showing Toast messages, etc.)
    protected void showToast(String message) {
        // You can implement a common method to show Toast in fragments
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }
}

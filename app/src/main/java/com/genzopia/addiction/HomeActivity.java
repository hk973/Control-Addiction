package com.genzopia.addiction;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class HomeActivity extends AppCompatActivity {
    private GestureDetectorCompat gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        setupStatusBar();
        setupGestureDetector();
        setupShortcuts();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupStatusBar() {
        Window window = getWindow();
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isSystemDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        int statusBarColor = isSystemDarkMode ?
                getResources().getColor(R.color.black, getTheme()) :
                getResources().getColor(R.color.white, getTheme());

        window.setStatusBarColor(statusBarColor);

        WindowInsetsControllerCompat windowInsetsController =
                new WindowInsetsControllerCompat(window, window.getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isSystemDarkMode);
    }

    private void setupShortcuts() {
        ImageButton phoneButton = findViewById(R.id.phoneButton);
        ImageButton cameraButton = findViewById(R.id.cameraButton);

        phoneButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            startActivity(intent);
        });

        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivity(intent);
        });
    }

    private void setupGestureDetector() {
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 50;
            private static final int SWIPE_VELOCITY_THRESHOLD = 50;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY) &&
                        Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX < 0) { // Swipe Left
                        startActivity(new Intent(HomeActivity.this, MainActivity2.class));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}
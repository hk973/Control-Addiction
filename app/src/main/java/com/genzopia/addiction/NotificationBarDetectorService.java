package com.genzopia.addiction;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path; // Correct Path import
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.NonNull;

public class NotificationBarDetectorService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(@NonNull AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String className = event.getClassName().toString();
            Log.e("test55555",className);
            if (className.contains("FrameLayout")||className.contains("RecentsActivity")) {
                performGlobalAction(GLOBAL_ACTION_HOME);
//                performSwipeUp(); // Call the method
                Log.e("test55555","swipe");
            }
        }
    }

    @Override
    public void onInterrupt() {}

    private void performSwipeUp() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        Path swipePath = new Path();
        swipePath.moveTo(metrics.widthPixels / 2, 10);
        swipePath.lineTo(metrics.widthPixels / 2, metrics.heightPixels - 10);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0L, 250L));

        dispatchGesture(builder.build(), null, null);
    }
}
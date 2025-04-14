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
        SharedPrefHelper ss=new SharedPrefHelper(getApplicationContext());
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            boolean time=ss.getTimeActivateStatus();
            String className = event.getClassName().toString();
            String tt=  event.getPackageName().toString();
            Log.e("test55555",className+",,,,,,"+tt);
            if (className.contains("FrameLayout")||className.contains("RecentsActivity")) {
                if(time){
                performGlobalAction(GLOBAL_ACTION_HOME);}
            }
        }
    }

    @Override
    public void onInterrupt() {}


}
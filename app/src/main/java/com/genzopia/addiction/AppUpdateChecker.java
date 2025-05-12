package com.genzopia.addiction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;


public class AppUpdateChecker implements DefaultLifecycleObserver {

    private final Activity activity;
    private boolean updateChecked = false;
    private static final int REQUEST_CODE = 123;

    public AppUpdateChecker(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onStart(LifecycleOwner owner) {
        if (!updateChecked) {
            checkForUpdate(activity);
            updateChecked = true;
        }
    }

    private void checkForUpdate(Context context) {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            REQUEST_CODE
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

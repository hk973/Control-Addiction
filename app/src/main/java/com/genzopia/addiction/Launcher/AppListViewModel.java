package com.genzopia.addiction.Launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.DeadSystemException;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppListViewModel extends ViewModel {
    private static final String TAG = "AppListViewModel";
    private final MutableLiveData<List<AppItem_Dataclass>> appItemsLiveData = new MutableLiveData<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    public void loadApps(Context context) {
        executor.execute(() -> {
            List<AppItem_Dataclass> appItems = new ArrayList<>();
            try {
                PackageManager pm = context.getPackageManager();
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                // Query installed apps
                List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

                // Process results
                Set<String> addedPackages = new HashSet<>();
                for (ResolveInfo ri : resolveInfos) {
                    String packageName = ri.activityInfo.packageName;
                    if (addedPackages.contains(packageName)) continue;

                    String appName = ri.loadLabel(pm).toString();
                    appItems.add(new AppItem_Dataclass(appName, packageName));
                    addedPackages.add(packageName);
                }

                // Sort alphabetically
                Collections.sort(appItems, (o1, o2) ->
                        o1.getName().compareToIgnoreCase(o2.getName()));

            } catch (RuntimeException e) {
                // Handle system-level exceptions (including DeadSystemException)
                if (e.getCause() instanceof DeadSystemException) {
                    Log.e(TAG, "System is shutting down: " + e.getMessage());
                } else {
                    Log.e(TAG, "Failed to load apps: " + e.getMessage(), e);
                }
                appItems = new ArrayList<>(); // Return empty list on error
            }

            appItemsLiveData.postValue(appItems);
        });
    }

    public LiveData<List<AppItem_Dataclass>> getAppItemsLiveData() {
        return appItemsLiveData;
    }
}
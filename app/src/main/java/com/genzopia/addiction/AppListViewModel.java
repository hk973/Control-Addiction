package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppListViewModel extends ViewModel {
    private MutableLiveData<List<AppItem_Dataclass>> appItemsLiveData = new MutableLiveData<>();
    private Executor executor = Executors.newSingleThreadExecutor();

    public void loadApps(Context context) {
        executor.execute(() -> {
            PackageManager pm = context.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

            List<AppItem_Dataclass> appItems = new ArrayList<>();
            Set<String> addedPackages = new HashSet<>();

            for (ResolveInfo ri : resolveInfos) {
                String packageName = ri.activityInfo.packageName;
                if (!addedPackages.contains(packageName)) {
                    String appName = ri.loadLabel(pm).toString();
                    appItems.add(new AppItem_Dataclass(appName, packageName));
                    addedPackages.add(packageName);
                }
            }

            Collections.sort(appItems, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            appItemsLiveData.postValue(appItems);
        });
    }

    public LiveData<List<AppItem_Dataclass>> getAppItemsLiveData() {
        return appItemsLiveData;
    }
}
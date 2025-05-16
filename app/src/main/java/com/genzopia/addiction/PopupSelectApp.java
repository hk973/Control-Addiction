package com.genzopia.addiction;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PopupSelectApp {

    private Context context;
    private Executor executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    public PopupSelectApp(Context context) {
        this.context = context;
    }

    public void show(ImageView cameraButton) {
        // Create dialog
        SharedPrefHelper ss=new SharedPrefHelper(context);

        if(ss.getString(context,"shortcut","")==""||ss.getString(context,"shortcut","0")==null){
            final Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.popup_app_selector); // You'll need to create this layout

            // Initialize RecyclerView
            RecyclerView recyclerView = dialog.findViewById(R.id.recyclerViewApps);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            // Show loading state if needed
            // You can add a progress bar in your layout and show/hide it here

            // Load apps in background
            executor.execute(() -> {
                // Load apps in background
                List<AppItem> appItems = loadInstalledApps();

                handler.post(() -> {
                    AppListAdapter adapter = new AppListAdapter(appItems, appItem -> {
                        // UI thread (for Toast and RecyclerView updates)
                        Toast.makeText(context, appItem.getPackageName(), Toast.LENGTH_SHORT).show();
                        cameraButton.setImageDrawable(appItem.getIcon()); // UI update
                        dialog.dismiss();

                        // Save data in background (move saving logic here)
                        executor.execute(() -> {
                            ss.saveString(context, "shortcut", appItem.getPackageName());
                        });
                    });
                    recyclerView.setAdapter(adapter);
                });
            });

            dialog.show();
        }else{
            openapp(ss.getString(context,"shortcut",""));
        }

    }
    public void show2(ImageView cameraButton) {
        SharedPrefHelper ss = new SharedPrefHelper(context);
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_app_selector);

        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerViewApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        executor.execute(() -> {
            // Load apps in background
            List<AppItem> appItems = loadInstalledApps();

            handler.post(() -> {
                AppListAdapter adapter = new AppListAdapter(appItems, appItem -> {
                    // UI thread (for Toast and RecyclerView updates)
                    Toast.makeText(context, appItem.getPackageName(), Toast.LENGTH_SHORT).show();
                    cameraButton.setImageDrawable(appItem.getIcon()); // UI update
                    dialog.dismiss();

                    // Save data in background (move saving logic here)
                    executor.execute(() -> {
                        ss.saveString(context, "shortcut", appItem.getPackageName());
                    });
                });
                recyclerView.setAdapter(adapter);
            });
        });

        dialog.show();
    }
    private boolean openapp(String packageName) {
        PackageManager pm = context.getPackageManager();

        try {
            // Check if the package exists
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);

            // Launch the app
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                // Add flags to clear the back stack if needed
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                return true;
            } else {
                // No launcher activity found (might be a service or other component)
                Toast.makeText(context, "App doesn't have a launcher activity", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // App not found
            Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show();
            return false;
        }

    }

    private List<AppItem> loadInstalledApps() {
        List<AppItem> appItems = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

        Set<String> addedPackages = new HashSet<>();

        for (ResolveInfo ri : resolveInfos) {
            String packageName = ri.activityInfo.packageName;
            if (!addedPackages.contains(packageName)) {
                String appName = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                appItems.add(new AppItem(appName, packageName, icon));
                addedPackages.add(packageName);
            }
        }

        // Sort alphabetically
        appItems.sort((o1, o2) -> o1.getAppName().compareToIgnoreCase(o2.getAppName()));

        return appItems;
    }
}
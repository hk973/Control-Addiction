package com.genzopia.addiction.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.genzopia.addiction.data.model.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Process-wide cache of the launchable applications installed on the device.
 * It is the single source of truth for every app list in the launcher and keeps
 * itself up to date through a package add/remove/replace broadcast receiver.
 */
public class AppRepository {

    private static final String TAG = "AppRepository";
    /** Bursts of package broadcasts (e.g. a Play Store batch update) are coalesced. */
    private static final long REFRESH_DEBOUNCE_MS = 500;

    private static volatile AppRepository instance;

    private final Context appContext;
    private final MutableLiveData<List<AppInfo>> appsLiveData = new MutableLiveData<>();
    private final Map<String, String> labelCache = new ConcurrentHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "AppRepository");
        thread.setDaemon(true);
        return thread;
    });

    private volatile List<AppInfo> cache = Collections.emptyList();
    private BroadcastReceiver packageReceiver;

    private final Runnable refreshTask = this::refresh;

    private AppRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static AppRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AppRepository.class) {
                if (instance == null) {
                    instance = new AppRepository(context);
                }
            }
        }
        return instance;
    }

    public LiveData<List<AppInfo>> getApps() {
        return appsLiveData;
    }

    /** Last known list, may be empty before the first load completes. */
    public List<AppInfo> getCachedApps() {
        return cache;
    }

    /** Reloads in the background and publishes only when the content actually changed. */
    public void refresh() {
        executor.execute(this::reload);
    }

    /** Coalesces a burst of package broadcasts into a single refresh. */
    public void refreshDebounced() {
        mainHandler.removeCallbacks(refreshTask);
        mainHandler.postDelayed(refreshTask, REFRESH_DEBOUNCE_MS);
    }

    /**
     * Returns the cached list, loading it synchronously when it is still empty.
     * Must never be called from the main thread.
     */
    public List<AppInfo> getAppsBlocking() {
        if (cache.isEmpty()) {
            reload();
        }
        return cache;
    }

    public List<String> getAllPackages() {
        List<AppInfo> current = cache;
        List<String> packages = new ArrayList<>(current.size());
        for (AppInfo app : current) {
            packages.add(app.getPackageName());
        }
        return packages;
    }

    /** Human readable label of a package, backed by an in-memory cache. */
    public String getLabel(String packageName) {
        if (packageName == null) return "";
        String cached = labelCache.get(packageName);
        if (cached != null) return cached;

        try {
            PackageManager pm = appContext.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            String label = pm.getApplicationLabel(info).toString();
            labelCache.put(packageName, label);
            return label;
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        } catch (RuntimeException e) {
            logSystemFailure("Failed to load label for " + packageName, e);
            return packageName;
        }
    }

    /** Builds AppInfo entries for the given packages, resolving labels from the cache. */
    public List<AppInfo> toAppInfos(List<String> packageNames) {
        List<AppInfo> result = new ArrayList<>();
        if (packageNames == null) return result;
        PackageManager pm = appContext.getPackageManager();
        for (String packageName : packageNames) {
            result.add(new AppInfo(getLabel(packageName), packageName, pm));
        }
        return result;
    }

    /**
     * Registers the single package-change receiver. Package broadcasts are protected
     * system broadcasts, so NOT_EXPORTED is enough for them to be delivered.
     */
    public void registerPackageReceiver() {
        if (packageReceiver != null) return;
        try {
            packageReceiver = new PackageChangeReceiver();

            IntentFilter packageFilter = new IntentFilter();
            packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            packageFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            packageFilter.addDataScheme("package");
            ContextCompat.registerReceiver(appContext, packageReceiver, packageFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED);

            IntentFilter suspendFilter = new IntentFilter();
            suspendFilter.addAction(Intent.ACTION_PACKAGES_SUSPENDED);
            suspendFilter.addAction(Intent.ACTION_PACKAGES_UNSUSPENDED);
            ContextCompat.registerReceiver(appContext, packageReceiver, suspendFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED);
        } catch (Exception e) {
            packageReceiver = null;
            Log.e(TAG, "Failed to register package receiver", e);
        }
    }

    private void reload() {
        List<AppInfo> loaded = queryApps();
        if (loaded == null) return; // system failure: keep the previous cache

        if (hasSameContent(cache, loaded)) return;
        cache = loaded;
        appsLiveData.postValue(loaded);
    }

    /** Returns null when the package manager could not be queried. */
    private List<AppInfo> queryApps() {
        try {
            PackageManager pm = appContext.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

            List<AppInfo> apps = new ArrayList<>();
            Set<String> added = new HashSet<>();
            for (ResolveInfo ri : resolveInfos) {
                String packageName = ri.activityInfo.packageName;
                if (!added.add(packageName)) continue;

                String label = ri.loadLabel(pm).toString();
                labelCache.put(packageName, label);
                apps.add(new AppInfo(label, packageName, pm));
            }
            return sortByLabel(apps);
        } catch (RuntimeException e) {
            logSystemFailure("Failed to load apps", e);
            return null;
        }
    }

    private void logSystemFailure(String message, RuntimeException e) {
        // DeadSystemException arrives wrapped in a RuntimeException while the system shuts down.
        Log.e(TAG, message + ": " + e.getMessage());
    }

    static List<AppInfo> sortByLabel(List<AppInfo> apps) {
        Collections.sort(apps, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
        return apps;
    }

    /** Compares package names and labels, since AppInfo.equals only looks at the package. */
    static boolean hasSameContent(List<AppInfo> oldList, List<AppInfo> newList) {
        if (oldList == null || newList == null) return oldList == newList;
        if (oldList.size() != newList.size()) return false;
        for (int i = 0; i < oldList.size(); i++) {
            AppInfo oldApp = oldList.get(i);
            AppInfo newApp = newList.get(i);
            if (!oldApp.getPackageName().equals(newApp.getPackageName())) return false;
            if (!oldApp.getLabel().equals(newApp.getLabel())) return false;
        }
        return true;
    }

    private class PackageChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getData() == null) {
                refreshDebounced();
                return;
            }
            String changed = intent.getData().getSchemeSpecificPart();
            if (changed != null) {
                labelCache.remove(changed);
            }
            refreshDebounced();
        }
    }
}

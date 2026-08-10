package com.genzopia.addiction.data;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks per-app screen time using {@link UsageStatsManager}.
 *
 * <p>All data is stored in "AddictionPrefs" under the key {@code usage_daily_json}
 * as a JSON map of {packageName → minutesUsedToday}. The date is stored under
 * {@code usage_date} (epoch-day). When a new day starts the old map is cleared so
 * SharedPrefs do not grow indefinitely.</p>
 *
 * <p>Usage stats require the {@code PACKAGE_USAGE_STATS} AppOps permission, which the
 * user must grant in Settings. Call {@link #hasPermission(Context)} before querying.</p>
 */
public class AppUsageTracker {

    private static final String TAG = "AppUsageTracker";
    private static final String PREF_NAME = "AddictionPrefs";
    private static final String KEY_USAGE_JSON = "usage_daily_json";
    private static final String KEY_USAGE_DATE = "usage_date";

    /** Minimum usage to include an app in the list (1 second). */
    private static final long MIN_USAGE_MS = 1_000L;

    // ─── Permission check ────────────────────────────────────────────────────

    /** Returns true when PACKAGE_USAGE_STATS has been granted. */
    public static boolean hasPermission(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        int mode = ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // ─── Core query ──────────────────────────────────────────────────────────

    /**
     * Queries usage stats for today and returns a list of {@link AppUsageStat} objects
     * sorted by usage time (most used first), excluding this app's own package.
     *
     * <p>Returns an empty list when the permission is missing.</p>
     */
    public static List<AppUsageStat> getTodayUsage(Context context) {
        if (!hasPermission(context)) return Collections.emptyList();

        UsageStatsManager usm = (UsageStatsManager)
                context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return Collections.emptyList();

        // Query from midnight today to now
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        long now = System.currentTimeMillis();

        List<UsageStats> rawStats;
        try {
            rawStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now);
        } catch (Exception e) {
            Log.e(TAG, "Failed to query usage stats", e);
            return Collections.emptyList();
        }

        if (rawStats == null || rawStats.isEmpty()) return Collections.emptyList();

        String ownPkg = context.getPackageName();
        PackageManager pm = context.getPackageManager();
        List<AppUsageStat> result = new ArrayList<>();

        for (UsageStats stat : rawStats) {
            String pkg = stat.getPackageName();
            long usedMs = stat.getTotalTimeInForeground();
            if (usedMs < MIN_USAGE_MS) continue;
            if (pkg.equals(ownPkg)) continue;

            // Only include apps that have a launcher icon (skip pure services/system)
            String label = pkg;
            try {
                label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (PackageManager.NameNotFoundException ignored) {
            }

            result.add(new AppUsageStat(pkg, label, usedMs));
        }

        // Sort by most used first
        Collections.sort(result, (a, b) -> Long.compare(b.totalTimeMs, a.totalTimeMs));

        // Persist to SharedPrefs (for display in app drawer without querying UsageStatsManager)
        persistTodayUsage(context, result);

        return result;
    }

    /** Returns the cached usage map {packageName → minutes} built by the last call to {@link #getTodayUsage}. */
    @SuppressWarnings("unchecked")
    public static Map<String, Long> getCachedUsageMinutes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long savedDay = prefs.getLong(KEY_USAGE_DATE, -1);
        long todayDay = currentEpochDay();

        if (savedDay != todayDay) {
            // Stale — return empty; caller should call getTodayUsage() for a fresh query
            return Collections.emptyMap();
        }

        String json = prefs.getString(KEY_USAGE_JSON, null);
        if (json == null) return Collections.emptyMap();

        try {
            Type type = new TypeToken<HashMap<String, Long>>() {}.getType();
            Map<String, Long> map = new Gson().fromJson(json, type);
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            Log.e(TAG, "Corrupt usage cache", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Returns the usage for a single package in minutes from today's cache.
     * Returns 0 if not found.
     */
    public static long getCachedMinutesForPackage(Context context, String packageName) {
        Map<String, Long> cache = getCachedUsageMinutes(context);
        Long minutes = cache.get(packageName);
        return minutes != null ? minutes : 0L;
    }

    // ─── Persistence ─────────────────────────────────────────────────────────

    private static void persistTodayUsage(Context context, List<AppUsageStat> stats) {
        Map<String, Long> map = new HashMap<>();
        for (AppUsageStat stat : stats) {
            map.put(stat.packageName, stat.totalTimeMs / 60_000L); // ms → minutes
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USAGE_JSON, new Gson().toJson(map))
                .putLong(KEY_USAGE_DATE, currentEpochDay())
                .apply();
    }

    private static long currentEpochDay() {
        return System.currentTimeMillis() / (24 * 60 * 60 * 1000L);
    }

    // ─── Data model ──────────────────────────────────────────────────────────

    /** Represents the screen-time of one app for a given period. */
    public static class AppUsageStat {
        public final String packageName;
        public final String label;
        public final long totalTimeMs;

        public AppUsageStat(String packageName, String label, long totalTimeMs) {
            this.packageName = packageName;
            this.label = label;
            this.totalTimeMs = totalTimeMs;
        }

        /** Human-readable time string, e.g. "1h 23m" or "45m". */
        public String getFormattedTime() {
            long totalMinutes = totalTimeMs / 60_000L;
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            if (hours > 0) {
                return hours + "h " + minutes + "m";
            }
            return minutes + "m";
        }
    }
}

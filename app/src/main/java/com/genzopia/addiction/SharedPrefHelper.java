package com.genzopia.addiction;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SharedPrefHelper {

    private static final String PREF_NAME = "AddictionPrefs";
    private static final String KEY_SELECTED_APPS = "selectedApp";
    private static final String KEY_TIME_LIMIT = "timeLimit";
    private static final String KEY_TIME_ACTIVE = "timeActive";
    private static final String CLICK_TO_OPEN = "click_to_open";
    private static final String KEY_DARK_MODE = "DarkMode";

    private static final String KEY_GRAY_MODE = "GrayMode";
    private static final String KEY_MODE_NIGHT = "modeNight";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    // Save if Gray Mode with Dark Mode is enabled
    public void setGrayModeWithDarkMode(boolean enabled) {
        editor.putBoolean(KEY_GRAY_MODE, enabled);
        editor.apply();
    }
    public void savePinnedApps(List<String> newPinnedApps) {
        List<String> current = getPinnedApps();
        current.addAll(newPinnedApps);

        // Remove duplicates while preserving order
        Set<String> unique = new LinkedHashSet<>(current);
        editor.putString("pinned_apps", TextUtils.join(",", unique)).apply();
    }

    // Updated getPinnedApps()
    public List<String> getPinnedApps() {
        String pinned = prefs.getString("pinned_apps", "");
        if (pinned.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(TextUtils.split(pinned, ",")));
    }

    // Updated setPinnedApps()
    public void setPinnedApps(List<String> pinnedApps) {
        Set<String> unique = new LinkedHashSet<>(pinnedApps);
        // Remove any accidental empty strings
        unique.removeIf(TextUtils::isEmpty);
        editor.putString("pinned_apps", TextUtils.join(",", unique)).apply();
    }
    // Get if Gray Mode with Dark Mode is enabled
    public boolean isGrayModeWithDarkModeEnabled() {
        return prefs.getBoolean(KEY_GRAY_MODE, false);
    }

    // Save Mode Night Preference (Yes or No)
    public void setModeNight(boolean isNight) {
        editor.putBoolean(KEY_MODE_NIGHT, isNight);
        editor.apply();
    }

    // Get Mode Night Preference
    public boolean isModeNightEnabled() {
        return prefs.getBoolean(KEY_MODE_NIGHT, false);
    }

    public boolean isGrayModeEnabled() {
        return prefs.getBoolean(KEY_GRAY_MODE, false);
    }

    public void setGrayModeEnabled(boolean enabled) {
        editor.putBoolean(KEY_GRAY_MODE, enabled);
        editor.apply();
        Log.d("SharedPrefDebug", "Gray Mode preference set to: " + enabled);
    }


    public SharedPrefHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void setClickToOpen(boolean isClick) {
        editor.putBoolean(CLICK_TO_OPEN, isClick);
        editor.apply();
    }

    public boolean isClickToOpen() {
        return prefs.getBoolean(CLICK_TO_OPEN, true);
    }

    public ArrayList<String> getSelectedAppValue() {
        String jsonSelectedApps = prefs.getString(KEY_SELECTED_APPS, null);
        Log.d("SharedPrefDebug", "Retrieved JSON: " + jsonSelectedApps);

        if (jsonSelectedApps == null || jsonSelectedApps.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return new Gson().fromJson(jsonSelectedApps, type);
    }
    public ArrayList<String> appwithnowarning(){
        ArrayList<String> selectedApp=new ArrayList<>();
        selectedApp.add("com.genzopia.addiction");
        selectedApp.add("com.android.systemui");
        selectedApp.add("com.sec.android.app.launcher");
        selectedApp.add("com.android.launcher");
        selectedApp.add("android");
        selectedApp.add("com.android.vending");
        // upi system
        selectedApp.add("com.google.android.apps.nbu.paisa.user");
        selectedApp.add("net.one97.paytm");
        selectedApp.add("com.phonepe.app");
        selectedApp.add("in.org.npci.upiapp");
        selectedApp.add("com.google.android.gms");

        return selectedApp;

    }

    public ArrayList<String> appWithNoWarning() {
        ArrayList<String> selectedApp = new ArrayList<>();
        selectedApp.add("com.genzopia.addiction");
        selectedApp.add("com.android.systemui");
        selectedApp.add("com.sec.android.app.launcher");
        selectedApp.add("com.android.launcher");
        selectedApp.add("android");
        selectedApp.add("com.android.vending");
        selectedApp.add("com.google.android.apps.nbu.paisa.user");
        selectedApp.add("net.one97.paytm");
        selectedApp.add("com.phonepe.app");
        selectedApp.add("in.org.npci.upiapp");
        selectedApp.add("com.google.android.gms");

        return selectedApp;
    }

    public int getTimeLimitValue() {
        return prefs.getInt(KEY_TIME_LIMIT, 0);
    }

    public void writeData(ArrayList<String> selectedApps, int timeLimit, boolean isActive) {
        Gson gson = new Gson();
        String jsonSelectedApps = gson.toJson(selectedApps);
        Log.d("SharedPrefDebug", "JSON to Store: " + jsonSelectedApps);
       Log.e("test444", String.valueOf(timeLimit));
        editor.putString(KEY_SELECTED_APPS, jsonSelectedApps);
        editor.putInt(KEY_TIME_LIMIT, timeLimit);
        editor.putBoolean(KEY_TIME_ACTIVE, isActive);
        editor.apply();
    }

    public void saveTimeLimitValue(int timeLimit) {
        editor.putInt(KEY_TIME_LIMIT, timeLimit);
        editor.apply();
    }

    public void saveTimeActivateStatus(boolean isActive) {
        editor.putBoolean(KEY_TIME_ACTIVE, isActive);
        editor.apply();
    }

    public boolean getReviewShown() {
        return prefs.getBoolean("review_shown", false);
    }

    public void setReviewShown(boolean shown) {
        prefs.edit().putBoolean("review_shown", shown).apply();
    }

    public void saveStartTime(long startTime) {
        prefs.edit().putLong("startTime", startTime).apply();
    }

    public void setTimeActivateStatus(boolean status) {
        editor.putBoolean(KEY_TIME_ACTIVE, status);
        editor.apply();
    }

    public long getStartTime() {
        return prefs.getLong("startTime", 0);
    }

    public void saveInitialDuration(int duration) {
        prefs.edit().putInt("initialDuration", duration).apply();
    }

    public int getInitialDuration() {
        return prefs.getInt("initialDuration", 0);
    }

    public boolean getTimeActivateStatus() {
        boolean isActive = prefs.getBoolean(KEY_TIME_ACTIVE, false);
        if (isActive) {
            long startTime = getStartTime();
            int initialDuration = getInitialDuration();
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;
            long durationMillis = initialDuration * 1000L;

            if (elapsed >= durationMillis) {
                setTimeActivateStatus(false);
                return false;
            }
        }
        return isActive;
    }

    public long getRemainingTimeMillis() {
        long startTime = getStartTime();
        int initialDuration = getInitialDuration();
        long durationMillis = initialDuration * 1000L;
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(durationMillis - elapsed, 0);
    }

    private static final String KEY_FOLLOW_SYSTEM_THEME = "FollowSystemTheme";

    // Save Follow System Preference
    public void setFollowSystemThemeEnabled(boolean isEnabled) {
        editor.putBoolean(KEY_FOLLOW_SYSTEM_THEME, isEnabled);
        editor.apply();
        Log.d("SharedPrefDebug", "Follow System Theme preference set to: " + isEnabled);
    }

    // Get Follow System Preference
    public boolean isFollowSystemThemeEnabled() {
        return prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, false); // default to false (i.e., not following system theme)
    }

    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(KEY_DARK_MODE, false); // default to light mode
    }

    public void setDarkModeEnabled(boolean isEnabled) {
        editor.putBoolean(KEY_DARK_MODE, isEnabled);
        editor.apply();
        Log.d("SharedPrefDebug", "Dark Mode preference set to: " + isEnabled);
    }
    private static final String PREFS_NAME = "MyAppPrefs";

    // Store a string in SharedPreferences
    public  void saveString(Context context, String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply(); // or use commit() if you need immediate result
    }

    // Retrieve a string from SharedPreferences
    public String getString(Context context, String key, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, defaultValue);
    }
}



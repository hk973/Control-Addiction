package com.genzopia.addiction;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class SharedPrefHelper {

    private static final String PREF_NAME = "AddictionPrefs";
    private static final String KEY_SELECTED_APPS = "selectedApp";
    private static final String KEY_TIME_LIMIT = "timeLimit";
    private static final String KEY_TIME_ACTIVE = "timeActive";
    private static final String CLICK_TO_OPEN = "click_to_open";
    private static final String KEY_DARK_MODE = "DarkMode";

    private static final String KEY_GRAY_MODE = "GrayMode";

    public boolean isGrayModeEnabled() {
        return prefs.getBoolean(KEY_GRAY_MODE, false);
    }

    public void setGrayModeEnabled(boolean enabled) {
        editor.putBoolean(KEY_GRAY_MODE, enabled).apply();
    }


    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

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

    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(KEY_DARK_MODE, false); // default to light mode
    }

    public void setDarkModeEnabled(boolean isEnabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isEnabled).apply();
    }
}

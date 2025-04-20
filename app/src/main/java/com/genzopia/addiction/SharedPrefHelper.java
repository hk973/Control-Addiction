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

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SharedPrefHelper(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = sharedPreferences.edit();
    }

    public void setClickToOpen(boolean isClick) {
        editor.putBoolean(CLICK_TO_OPEN, isClick);
        editor.apply();
    }

    public boolean isClickToOpen() {
        return sharedPreferences.getBoolean(CLICK_TO_OPEN, true);
    }

    public ArrayList<String> getSelectedAppValue() {
        String jsonSelectedApps = sharedPreferences.getString(KEY_SELECTED_APPS, null);
        Log.d("SharedPrefDebug", "Retrieved JSON: " + jsonSelectedApps);

        if (jsonSelectedApps == null || jsonSelectedApps.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<ArrayList<String>>() {}.getType();
       ArrayList<String> selected= new Gson().fromJson(jsonSelectedApps, type);
       


        return selected;
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

    public int getTimeLimitValue() {
        return sharedPreferences.getInt(KEY_TIME_LIMIT, 0);
    }

    public boolean getTimeActivateStatus() {
        return sharedPreferences.getBoolean(KEY_TIME_ACTIVE, false);
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
        return sharedPreferences.getBoolean("review_shown", false);
    }

    public void setReviewShown(boolean shown) {
        sharedPreferences.edit().putBoolean("review_shown", shown).apply();
    }
    public void saveStartTime(long startTime) {
        sharedPreferences.edit().putLong("startTime", startTime).apply();
    }

    public long getStartTime() {
        return sharedPreferences.getLong("startTime", 0);
    }

    public void saveInitialDuration(int duration) {
        sharedPreferences.edit().putInt("initialDuration", duration).apply();
    }

    public int getInitialDuration() {
        return sharedPreferences.getInt("initialDuration", 0);
    }
}
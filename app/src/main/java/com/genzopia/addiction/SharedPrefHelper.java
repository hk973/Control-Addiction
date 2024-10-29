package com.genzopia.addiction;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class SharedPrefHelper {

    private static final String PREF_NAME = "AddictionPrefs"; // Shared Preferences name
    private static final String KEY_SELECTED_APPS = "selectedApp"; // Key for selected apps
    private static final String KEY_TIME_LIMIT = "timeLimit"; // Key for time limit
    private static final String KEY_TIME_ACTIVE = "timeActive"; // Key for timer active status

    private Context context;

    // Constructor
    public SharedPrefHelper(Context context) {
        this.context = context;
    }

    // Retrieve the list of selected apps from Shared Preferences
    public ArrayList<String> getSelectedAppValue() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonSelectedApps = sharedPreferences.getString(KEY_SELECTED_APPS, null);

        // Log the retrieved JSON string for debugging
        Log.d("SharedPrefDebug", "Retrieved JSON: " + jsonSelectedApps);

        // If the JSON string is null or empty, return an empty list
        if (jsonSelectedApps == null || jsonSelectedApps.isEmpty()) {
            return new ArrayList<>();
        }

        // Use Gson to convert the JSON string back to an ArrayList
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return new Gson().fromJson(jsonSelectedApps, type);
    }

    // Retrieve the time limit value
    public int getTimeLimitValue() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(KEY_TIME_LIMIT, 0); // Default to 0 if not set
    }

    // Check if the timer is active
    public boolean getTimeActivateStatus() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_TIME_ACTIVE, false); // Default to false if not set
    }

    // Write data to Shared Preferences
    public void writeData(ArrayList<String> selectedApps, int timeLimit, boolean isActive) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Convert the selected apps list to JSON for storage
        Gson gson = new Gson();
        String jsonSelectedApps = gson.toJson(selectedApps);

        Log.d("SharedPrefDebug", "JSON to Store: " + jsonSelectedApps);

        editor.putString(KEY_SELECTED_APPS, jsonSelectedApps);
        editor.putInt(KEY_TIME_LIMIT, timeLimit);
        editor.putBoolean(KEY_TIME_ACTIVE, isActive);
        editor.apply(); // Save changes
    }

    // Method to save the time limit value
    public void saveTimeLimitValue(int timeLimit) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_TIME_LIMIT, timeLimit);
        editor.apply(); // Save changes
    }

    // Method to save the timer active status
    public void saveTimeActivateStatus(boolean isActive) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_TIME_ACTIVE, isActive);
        editor.apply(); // Save changes
    }


}

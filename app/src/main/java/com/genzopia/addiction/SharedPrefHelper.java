package com.genzopia.addiction;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
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
    public void setChallengeStatus(Context context, boolean status) {
        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("Challenge_status", status)
                .apply();
    }
    public boolean getChallengeStatus(Context context) {
        return context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                .getBoolean("Challenge_status", false); // default is false
    }

    public void setGrayModeEnabled(boolean enabled) {
        editor.putBoolean(KEY_GRAY_MODE, enabled);
        editor.apply();
        Log.d("SharedPrefDebug", "Gray Mode preference set to: " + enabled);
    }
    public void setTermsAccepted(boolean accepted) {
        prefs.edit().putBoolean("terms_accepted", accepted).apply();
    }

    public boolean isTermsAccepted() {
        return prefs.getBoolean("terms_accepted", false);
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

    public ArrayList<String> appWithNoWarning() {
        ArrayList<String> selectedApp = new ArrayList<>();

        // Your app and essential system apps
        selectedApp.add("com.genzopia.addiction");
        selectedApp.add("com.android.systemui");
        selectedApp.add("com.sec.android.app.launcher");
        selectedApp.add("com.android.launcher");
        selectedApp.add("android");

        // Google Play-related apps (required for billing)
        selectedApp.add("com.android.vending");               // Google Play Store
        selectedApp.add("com.google.android.gms");            // Google Play Services

        // ------------------------------------------
        // 🇮🇳 India – UPI and wallet-based payments
        // ------------------------------------------
        selectedApp.add("com.google.android.apps.nbu.paisa.user"); // Google Pay (India UPI)
        selectedApp.add("net.one97.paytm");                        // Paytm
        selectedApp.add("com.phonepe.app");                        // PhonePe
        selectedApp.add("in.org.npci.upiapp");                     // BHIM UPI
        selectedApp.add("com.freecharge.android");                 // Freecharge
        selectedApp.add("com.mobikwik_new");                       // MobiKwik
        selectedApp.add("com.airtel.money.client");                // Airtel Payments
        selectedApp.add("com.amazon.mShop.android.shopping");      // Amazon Pay (India)

        // ------------------------------------------
        // 🇺🇸 United States
        // ------------------------------------------
        selectedApp.add("com.google.android.apps.walletnfcrel");   // Google Wallet (US)
        selectedApp.add("com.squareup.cash");                      // Cash App
        selectedApp.add("com.venmo");                              // Venmo
        selectedApp.add("com.zellepay.zelle");                     // Zelle

        // ------------------------------------------
        // 🇪🇺 Europe
        // ------------------------------------------
        selectedApp.add("de.number26.android");                    // N26
        selectedApp.add("com.revolut.revolut");                    // Revolut
        selectedApp.add("com.klarna.android");                     // Klarna

        // ------------------------------------------
        // 🇧🇷 Brazil
        // ------------------------------------------
        selectedApp.add("com.mercadopago.wallet");                 // MercadoPago
        selectedApp.add("com.nu.production");                      // Nubank

        // ------------------------------------------
        // 🌍 Africa (e.g., Nigeria, Kenya)
        // ------------------------------------------
        selectedApp.add("com.opay.opaycustomer");                  // OPay (Nigeria)
        selectedApp.add("com.flutterwave.rave");                   // Flutterwave
        selectedApp.add("com.mtn.momo");                           // MTN MoMo

        // ------------------------------------------
        // 🇨🇳 China
        // ------------------------------------------
        selectedApp.add("com.eg.android.AlipayGphone");            // Alipay
        selectedApp.add("com.tencent.mm");                         // WeChat Pay

        return selectedApp;
    }


    public long getTimeLimitValue() {
        return prefs.getLong(KEY_TIME_LIMIT, 0);
    }

    public void writeData(ArrayList<String> selectedApps, long timeLimit, boolean isActive) {
        Gson gson = new Gson();
        String jsonSelectedApps = gson.toJson(selectedApps);
        Log.d("SharedPrefDebug", "JSON to Store: " + jsonSelectedApps);
       Log.e("test444", String.valueOf(timeLimit));
        editor.putString(KEY_SELECTED_APPS, jsonSelectedApps);
        editor.putLong(KEY_TIME_LIMIT, timeLimit);
        editor.putBoolean(KEY_TIME_ACTIVE, isActive);
        editor.apply();
    }

    public void saveTimeLimitValue(long timeLimit) {
        editor.putLong(KEY_TIME_LIMIT, timeLimit);
        editor.apply();
    }

    public void saveTimeActivateStatus(boolean isActive) {
        editor.putBoolean(KEY_TIME_ACTIVE, isActive);
        editor.apply();
    }


    private static final String LAST_REVIEW_PROMPT_TIME = "last_review_prompt_time";
    private static final int DAYS_BETWEEN_REVIEWS = 2;

    // Store the last time review was prompted (in milliseconds)
    public void saveLastReviewPromptTime(long timeInMillis) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_REVIEW_PROMPT_TIME, timeInMillis);
        editor.apply();
    }

    // Get the last time review was prompted
    public long getLastReviewPromptTime() {
        return prefs.getLong(LAST_REVIEW_PROMPT_TIME, 0);
    }

    // Check if it's time to show review again (2+ days passed)
    public boolean shouldShowReview() {
        long lastPromptTime = getLastReviewPromptTime();

        // If never prompted before, show it
        if (lastPromptTime == 0) {
            return true;
        }

        // Calculate if 2 days (172800000 ms) have passed
        long currentTime = System.currentTimeMillis();
        long DaysInMillis = DAYS_BETWEEN_REVIEWS * 24 * 60 * 60 * 1000;

        return (currentTime - lastPromptTime) >= DaysInMillis;
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

    public void saveInitialDuration(long duration) {
        prefs.edit().putLong("initialDuration", duration).apply();
    }

    public long getInitialDuration() {
        return prefs.getLong("initialDuration", 0);
    }

    public boolean getTimeActivateStatus() {
        boolean isActive = prefs.getBoolean(KEY_TIME_ACTIVE, false);
        if (isActive) {
            long startTime = getStartTime();
            long initialDuration = getInitialDuration();
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
        long initialDuration = getInitialDuration();
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
    public void saveString(Context context, String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getString(Context context, String key, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, defaultValue);
    }



}



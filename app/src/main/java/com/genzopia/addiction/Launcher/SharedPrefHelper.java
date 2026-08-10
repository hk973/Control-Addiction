package com.genzopia.addiction.Launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Single access point to every persisted setting of the app.
 *
 * <p>State is spread over four preference files, kept as-is for backwards compatibility
 * with installed versions:</p>
 * <ul>
 *     <li><b>AddictionPrefs</b> ({@link #PREF_NAME}) — lock state ({@code timeActive},
 *     {@code timeLimit}, {@code startTime}, {@code initialDuration}), the allowed-app lists
 *     ({@code selectedApp}, {@code selectedApps}), home-screen {@code pinned_apps}, theming
 *     ({@code DarkMode}, {@code GrayMode}, {@code modeNight}, {@code FollowSystemTheme}),
 *     onboarding ({@code terms_accepted}), the review throttle and the whole LeetCode/DSA
 *     challenge state.</li>
 *     <li><b>MyPrefs</b> — challenge flags ({@code Challenge_status}, {@code cheatchallengevalue}).</li>
 *     <li><b>MySharedPref</b> — earned reward codes ({@code challenge_code}, JSON list).</li>
 *     <li><b>MyAppPrefs</b> ({@link #PREFS_NAME}) — FCM token, notification-permission flag and
 *     the home-screen {@code shortcut}.</li>
 * </ul>
 *
 * <p>Every write obtains a fresh {@link SharedPreferences.Editor}. A single editor shared by
 * all setters used to be cached here, which could publish or drop unrelated pending changes
 * when two writes interleaved.</p>
 */
public class SharedPrefHelper {

    private static final String PREF_NAME = "AddictionPrefs";
    private static final String KEY_SELECTED_APPS = "selectedApp";
    private static final String KEY_SELECTED_APPSS = "selectedApps";
    private static final String KEY_TIME_LIMIT = "timeLimit";
    private static final String KEY_TIME_ACTIVE = "timeActive";
    private static final String CLICK_TO_OPEN = "click_to_open";
    private static final String KEY_DARK_MODE = "DarkMode";

    private static final String KEY_GRAY_MODE = "GrayMode";
    private static final String KEY_MODE_NIGHT = "modeNight";

    private static final String challenge_code ="challenge_code";

    private final SharedPreferences prefs;

    /**
     * Enables gray mode as part of the "grey theme" choice. Writes the very same
     * {@code GrayMode} flag as {@link #setGrayModeEnabled(boolean)}; both are kept because
     * the onboarding flow and the settings screen each call their own variant.
     */
    public void setGrayModeWithDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_GRAY_MODE, enabled).apply();
    }
    public void setCheatChallengeValue(Context context, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("cheatchallengevalue", value);
        editor.apply();
    }
    public boolean getCheatChallengeValue(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("cheatchallengevalue", false); // false is the default value
    }
    public void savePinnedApps(List<String> newPinnedApps) {
        List<String> current = getPinnedApps();
        current.addAll(newPinnedApps);

        // Remove duplicates while preserving order
        Set<String> unique = new LinkedHashSet<>(current);
        prefs.edit().putString("pinned_apps", TextUtils.join(",", unique)).apply();
    }
    public static void set_challenge_code_List(Context context, String newItem) {
        SharedPreferences sharedPref = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String json = sharedPref.getString(challenge_code, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> list;

        if (json != null) {
            list = gson.fromJson(json, type);
        } else {
            list = new ArrayList<>();
        }

        list.add(newItem); // Append new item

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(challenge_code, gson.toJson(list));
        editor.apply();
    }

    public static ArrayList<String> get_challenge_code_list(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        String json = sharedPref.getString(challenge_code, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();

        if (json != null) {
            return gson.fromJson(json, type);
        } else {
            return new ArrayList<>();
        }
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
        prefs.edit().putString("pinned_apps", TextUtils.join(",", unique)).apply();
    }
    // Get if Gray Mode with Dark Mode is enabled
    public boolean isGrayModeWithDarkModeEnabled() {
        return prefs.getBoolean(KEY_GRAY_MODE, false);
    }

    // Save Mode Night Preference (Yes or No)
    public void setModeNight(boolean isNight) {
        prefs.edit().putBoolean(KEY_MODE_NIGHT, isNight).apply();
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

    /** Settings-screen variant of {@link #setGrayModeWithDarkMode(boolean)}; same stored flag. */
    public void setGrayModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GRAY_MODE, enabled).apply();
    }
    public void setTermsAccepted(boolean accepted) {
        prefs.edit().putBoolean("terms_accepted", accepted).apply();
    }

    public boolean isTermsAccepted() {
        return prefs.getBoolean("terms_accepted", false);
    }


    public SharedPrefHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setClickToOpen(boolean isClick) {
        prefs.edit().putBoolean(CLICK_TO_OPEN, isClick).apply();
    }

    public boolean isClickToOpen() {
        return prefs.getBoolean(CLICK_TO_OPEN, true);
    }

    public ArrayList<String> getSelectedAppValue() {
        return readPackageList(KEY_SELECTED_APPS);
    }

    public ArrayList<String> tempgetSelectedAppValue() {
        return readPackageList(KEY_SELECTED_APPSS);
    }

    /** Reads a Gson-encoded package list; a corrupt value is treated as "no apps". */
    private ArrayList<String> readPackageList(String key) {
        String json = prefs.getString(key, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            ArrayList<String> stored = new Gson().fromJson(json, type);
            return stored == null ? new ArrayList<>() : stored;
        } catch (RuntimeException e) {
            Log.e("SharedPrefHelper", "Corrupt package list for " + key, e);
            return new ArrayList<>();
        }
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
        String jsonSelectedApps = new Gson().toJson(selectedApps);
        // One atomic write: the allowed apps and the lock flag must never be published apart.
        prefs.edit()
                .putString(KEY_SELECTED_APPS, jsonSelectedApps)
                .putLong(KEY_TIME_LIMIT, timeLimit)
                .putBoolean(KEY_TIME_ACTIVE, isActive)
                .apply();
    }
    public void set_selectedApps(ArrayList<String> selectedApps) {
        // Clone the list to avoid ConcurrentModificationException
        ArrayList<String> safeList = new ArrayList<>(selectedApps);

        String jsonSelectedApps = new Gson().toJson(safeList);
        prefs.edit().putString(KEY_SELECTED_APPS, jsonSelectedApps).apply();
    }

    public void temp_set_selectedApps(ArrayList<String> selectedApps){
        String jsonSelectedApps = new Gson().toJson(new ArrayList<>(selectedApps));
        prefs.edit().putString(KEY_SELECTED_APPSS, jsonSelectedApps).apply();
    }


    public void saveTimeLimitValue(long timeLimit) {
        prefs.edit().putLong(KEY_TIME_LIMIT, timeLimit).apply();
    }

    public void saveTimeActivateStatus(boolean isActive) {
        prefs.edit().putBoolean(KEY_TIME_ACTIVE, isActive).apply();
    }


    private static final String LAST_REVIEW_PROMPT_TIME = "last_review_prompt_time";
    private static final int DAYS_BETWEEN_REVIEWS = 2;

    // Store the last time review was prompted (in milliseconds)
    public void saveLastReviewPromptTime(long timeInMillis) {
        prefs.edit().putLong(LAST_REVIEW_PROMPT_TIME, timeInMillis).apply();
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
        prefs.edit().putBoolean(KEY_TIME_ACTIVE, status).apply();
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
        prefs.edit().putBoolean(KEY_FOLLOW_SYSTEM_THEME, isEnabled).apply();
    }

    // Get Follow System Preference
    public boolean isFollowSystemThemeEnabled() {
        return prefs.getBoolean(KEY_FOLLOW_SYSTEM_THEME, false); // default to false (i.e., not following system theme)
    }

    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(KEY_DARK_MODE, false); // default to light mode
    }

    public void setDarkModeEnabled(boolean isEnabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isEnabled).apply();
    }
    private static final String PREFS_NAME = "MyAppPrefs";

    // FCM keys
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_NOTIF_PERMISSION_GRANTED = "notif_permission_granted";

    /** Save the latest FCM registration token. */
    public void saveFcmToken(Context context, String token) {
        saveString(context, KEY_FCM_TOKEN, token);
    }

    /** Retrieve the latest FCM registration token, or null if not yet stored. */
    public String getFcmToken(Context context) {
        return getString(context, KEY_FCM_TOKEN, null);
    }

    /** Persist whether the POST_NOTIFICATIONS permission has been granted. */
    public void setNotifPermissionGranted(Context context, boolean granted) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(KEY_NOTIF_PERMISSION_GRANTED, granted).apply();
    }

    /** Returns true if the POST_NOTIFICATIONS permission was previously granted. */
    public boolean isNotifPermissionGranted(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_NOTIF_PERMISSION_GRANTED, false);
    }

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
    public void setDSAChallengeActive(boolean active) {
        prefs.edit().putBoolean("dsa_challenge_active", active).apply();
    }

    public boolean isDSAChallengeActive() {
        return prefs.getBoolean("dsa_challenge_active", false);
    }
    public void set_current_leetcode(long score){
        prefs.edit().putLong("leetcode_score",score).apply();
    }
    public long get_current_leetcode(){
        return prefs.getLong("leetcode_score",-1);
    }
    // Store LeetCode username
    public void setLeetCodeUsername(String username) {
        prefs.edit().putString("leetcode_username", username).apply();
    }

    // Retrieve LeetCode username
    public String getLeetCodeUsername() {
        return prefs.getString("leetcode_username", null);
    }
    // Store DSA Challenge remaining time (in seconds)
    public void setDSAChallengeRemainingTime(long seconds) {
        prefs.edit().putLong("dsa_challenge_remaining_time", seconds).apply();
    }

    // Retrieve DSA Challenge remaining time (in seconds)
    public long getDSAChallengeRemainingTime() {
        return prefs.getLong("dsa_challenge_remaining_time", 0);
    }

    // Store time allocated per question (in seconds)
    public void setPerQuestionTime(long seconds) {
        prefs.edit().putLong("per_question_time", seconds).apply();
    }

    // Retrieve time allocated per question (in seconds)
    public long getPerQuestionTime() {
        return prefs.getLong("per_question_time", 0);
    }

    public boolean istimmerzero(){
        return prefs.getBoolean("is_timmer_zero", true);
    }
    public void settimmerzero(boolean b){
       prefs.edit().putBoolean("is_timmer_zero",b).apply();
    }



}



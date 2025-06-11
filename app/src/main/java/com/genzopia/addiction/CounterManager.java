package com.genzopia.addiction;

import android.content.Context;
import android.content.SharedPreferences;

public class CounterManager {
    private static final String PREF_NAME = "counter_prefs";
    private static final String COUNTER_KEY = "counter_value";
    private static final String Review_pref = "review_fref";
    private static final String Review_key = "review_key";
    private static final int MAX_COUNT = 10;
    SharedPreferences prefs;

    public int getCounter(Context context) {
         prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(COUNTER_KEY, 0);
    }

    public  int increment(Context context) {
       prefs= context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int current = prefs.getInt(COUNTER_KEY, 0);
        int newCount = (current >= MAX_COUNT) ? 0 : current + 1;
        prefs.edit().putInt(COUNTER_KEY, newCount).apply();
        return newCount;
    }
    public void setreview(Context con,Boolean bb){
        prefs= con.getSharedPreferences(Review_pref, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(Review_key,bb).apply();
    }
    public boolean getReview(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Review_pref, Context.MODE_PRIVATE);
        return prefs.getBoolean(Review_key, false); // false is the default value
    }

}


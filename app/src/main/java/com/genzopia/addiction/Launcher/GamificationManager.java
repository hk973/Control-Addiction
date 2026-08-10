package com.genzopia.addiction.Launcher;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Manages all gamification state: XP, levels, streaks, and badges.
 *
 * <p>All state lives in "AddictionPrefs" (to stay in one file) under these keys:</p>
 * <ul>
 *     <li>{@code gam_xp}          – total XP (long)</li>
 *     <li>{@code gam_level}       – current level (int, starts at 1)</li>
 *     <li>{@code gam_streak}      – current daily streak (int)</li>
 *     <li>{@code gam_last_date}   – epoch-day of last session (long)</li>
 *     <li>{@code gam_badges}      – JSON list of earned badge IDs (String)</li>
 *     <li>{@code gam_blocks_today}– number of blocked-app attempts today (int)</li>
 *     <li>{@code gam_sessions}    – total completed focus sessions (int)</li>
 * </ul>
 *
 * <p>Level thresholds follow a simple quadratic curve: level N requires
 * {@code N * N * 100} cumulative XP. The UI can call {@link #getLevelProgress(Context)}
 * to get a 0-100 percentage for the progress bar.</p>
 */
public class GamificationManager {

    private static final String PREF_NAME = "AddictionPrefs";

    // Keys (must never be renamed — architecture rule 2)
    static final String KEY_XP              = "gam_xp";
    static final String KEY_LEVEL           = "gam_level";
    static final String KEY_STREAK          = "gam_streak";
    static final String KEY_LAST_DATE       = "gam_last_date";
    static final String KEY_BADGES          = "gam_badges";
    static final String KEY_BLOCKS_TODAY    = "gam_blocks_today";
    static final String KEY_SESSIONS        = "gam_sessions";
    static final String KEY_BLOCKS_DATE     = "gam_blocks_date";

    // XP awards
    public static final int XP_PER_BLOCK    = 5;
    public static final int XP_SESSION_DONE = 50;
    public static final int XP_STREAK_BONUS = 20;

    // ─── Badge IDs ───────────────────────────────────────────────────────────
    public static final String BADGE_FIRST_BLOCK    = "first_block";
    public static final String BADGE_100_BLOCKS     = "blocks_100";
    public static final String BADGE_STREAK_3       = "streak_3";
    public static final String BADGE_STREAK_7       = "streak_7";
    public static final String BADGE_STREAK_30      = "streak_30";
    public static final String BADGE_FIRST_SESSION  = "first_session";
    public static final String BADGE_SESSIONS_10    = "sessions_10";
    public static final String BADGE_LEVEL_5        = "level_5";
    public static final String BADGE_LEVEL_10       = "level_10";

    // ─── Public API ──────────────────────────────────────────────────────────

    /** Call every time a blocked-app attempt is detected. */
    public static void onAppBlocked(Context context) {
        SharedPreferences prefs = prefs(context);
        SharedPreferences.Editor ed = prefs.edit();

        // Reset daily block counter on a new day
        long today = epochDay();
        long blocksDate = prefs.getLong(KEY_BLOCKS_DATE, -1);
        int blocksToday = (blocksDate == today) ? prefs.getInt(KEY_BLOCKS_TODAY, 0) : 0;
        blocksToday++;
        ed.putInt(KEY_BLOCKS_TODAY, blocksToday);
        ed.putLong(KEY_BLOCKS_DATE, today);

        // Award XP
        long xp = prefs.getLong(KEY_XP, 0) + XP_PER_BLOCK;
        ed.putLong(KEY_XP, xp);
        ed.apply();

        // Check level up
        checkAndUpdateLevel(context);

        // Check badges
        checkBadge(context, BADGE_FIRST_BLOCK, 1, getTotalBlocks(context));
        checkBadge(context, BADGE_100_BLOCKS, 100, getTotalBlocks(context));
    }

    /** Call when a focus session completes normally (timer reaches zero). */
    public static void onSessionCompleted(Context context) {
        SharedPreferences prefs = prefs(context);

        int sessions = prefs.getInt(KEY_SESSIONS, 0) + 1;
        long xp = prefs.getLong(KEY_XP, 0) + XP_SESSION_DONE;

        // Update streak
        long today = epochDay();
        long lastDate = prefs.getLong(KEY_LAST_DATE, -1);
        int streak = prefs.getInt(KEY_STREAK, 0);

        if (lastDate == today - 1) {
            // Consecutive day
            streak++;
            xp += XP_STREAK_BONUS;
        } else if (lastDate != today) {
            // Gap in streak or first ever
            streak = 1;
        }
        // If lastDate == today, session already counted for today's streak

        prefs.edit()
                .putInt(KEY_SESSIONS, sessions)
                .putLong(KEY_XP, xp)
                .putInt(KEY_STREAK, streak)
                .putLong(KEY_LAST_DATE, today)
                .apply();

        checkAndUpdateLevel(context);
        checkBadge(context, BADGE_FIRST_SESSION, 1, sessions);
        checkBadge(context, BADGE_SESSIONS_10, 10, sessions);
        checkBadge(context, BADGE_STREAK_3, 3, streak);
        checkBadge(context, BADGE_STREAK_7, 7, streak);
        checkBadge(context, BADGE_STREAK_30, 30, streak);
    }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public static long getXP(Context context) {
        return prefs(context).getLong(KEY_XP, 0);
    }

    public static int getLevel(Context context) {
        return prefs(context).getInt(KEY_LEVEL, 1);
    }

    public static int getStreak(Context context) {
        SharedPreferences prefs = prefs(context);
        // If last session was not today or yesterday the streak is broken
        long today = epochDay();
        long lastDate = prefs.getLong(KEY_LAST_DATE, -1);
        if (lastDate < today - 1 && lastDate != -1) {
            // Streak broken — reset lazily
            prefs.edit().putInt(KEY_STREAK, 0).apply();
            return 0;
        }
        return prefs.getInt(KEY_STREAK, 0);
    }

    public static int getSessions(Context context) {
        return prefs(context).getInt(KEY_SESSIONS, 0);
    }

    /**
     * Returns 0-100 progress within the current level toward the next level.
     * At level cap it returns 100.
     */
    public static int getLevelProgress(Context context) {
        long xp = getXP(context);
        int level = getLevel(context);
        long xpForCurrent = xpRequiredForLevel(level);
        long xpForNext = xpRequiredForLevel(level + 1);
        if (xpForNext <= xpForCurrent) return 100;
        long intoLevel = xp - xpForCurrent;
        long levelRange = xpForNext - xpForCurrent;
        return (int) Math.min(100, (intoLevel * 100) / levelRange);
    }

    /** XP required to reach the given level (cumulative). Level 1 requires 0 XP. */
    public static long xpRequiredForLevel(int level) {
        if (level <= 1) return 0;
        return (long) (level - 1) * (level - 1) * 100L;
    }

    public static List<String> getEarnedBadges(Context context) {
        String json = prefs(context).getString(KEY_BADGES, null);
        if (json == null) return new ArrayList<>();
        try {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            List<String> list = new Gson().fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static boolean hasBadge(Context context, String badgeId) {
        return getEarnedBadges(context).contains(badgeId);
    }

    /** Returns a human-readable title for the current level. */
    public static String getLevelTitle(Context context) {
        int level = getLevel(context);
        if (level <= 2)  return "Beginner 🌱";
        if (level <= 5)  return "Focused 🎯";
        if (level <= 10) return "Disciplined 🔥";
        if (level <= 20) return "Master 🏆";
        return "Legend 👑";
    }

    /** Returns emoji label for a badge ID. */
    public static String getBadgeEmoji(String badgeId) {
        switch (badgeId) {
            case BADGE_FIRST_BLOCK:   return "🚫";
            case BADGE_100_BLOCKS:    return "💯";
            case BADGE_STREAK_3:      return "🔥";
            case BADGE_STREAK_7:      return "⚡";
            case BADGE_STREAK_30:     return "🏆";
            case BADGE_FIRST_SESSION: return "⏱️";
            case BADGE_SESSIONS_10:   return "🎯";
            case BADGE_LEVEL_5:       return "⭐";
            case BADGE_LEVEL_10:      return "💎";
            default:                  return "🏅";
        }
    }

    /** Returns the title for a badge ID. */
    public static String getBadgeTitle(String badgeId) {
        switch (badgeId) {
            case BADGE_FIRST_BLOCK:   return "First Block";
            case BADGE_100_BLOCKS:    return "Century Blocker";
            case BADGE_STREAK_3:      return "3-Day Streak";
            case BADGE_STREAK_7:      return "Week Warrior";
            case BADGE_STREAK_30:     return "Month Master";
            case BADGE_FIRST_SESSION: return "First Session";
            case BADGE_SESSIONS_10:   return "10 Sessions";
            case BADGE_LEVEL_5:       return "Level 5";
            case BADGE_LEVEL_10:      return "Level 10";
            default:                  return "Achievement";
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private static void checkAndUpdateLevel(Context context) {
        SharedPreferences prefs = prefs(context);
        long xp = prefs.getLong(KEY_XP, 0);
        int currentLevel = prefs.getInt(KEY_LEVEL, 1);

        // Find the highest level that xp qualifies for
        int newLevel = 1;
        for (int l = 1; l <= 50; l++) {
            if (xp >= xpRequiredForLevel(l)) newLevel = l;
            else break;
        }

        if (newLevel > currentLevel) {
            prefs.edit().putInt(KEY_LEVEL, newLevel).apply();
            // Check level badges
            checkBadge(context, BADGE_LEVEL_5, 5, newLevel);
            checkBadge(context, BADGE_LEVEL_10, 10, newLevel);
        }
    }

    private static void checkBadge(Context context, String badgeId, int threshold, long value) {
        if (value >= threshold && !hasBadge(context, badgeId)) {
            awardBadge(context, badgeId);
        }
    }

    private static void awardBadge(Context context, String badgeId) {
        List<String> badges = getEarnedBadges(context);
        if (!badges.contains(badgeId)) {
            badges.add(badgeId);
            prefs(context).edit()
                    .putString(KEY_BADGES, new Gson().toJson(badges))
                    .apply();
        }
    }

    private static long getTotalBlocks(Context context) {
        // Approximate total from daily counter + sessions; good enough for badge checks
        return prefs(context).getLong(KEY_XP, 0) / XP_PER_BLOCK;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static long epochDay() {
        return System.currentTimeMillis() / (24 * 60 * 60 * 1000L);
    }
}

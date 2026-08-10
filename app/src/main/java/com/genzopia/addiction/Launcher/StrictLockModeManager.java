package com.genzopia.addiction.Launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

/**
 * Manages the "strict lock mode" — extra friction the user must overcome to
 * manually stop a focus session before the timer expires.
 *
 * <h3>Modes</h3>
 * <ul>
 *   <li>{@link #MODE_NORMAL}         – one tap "Go Back to Work" (existing behaviour)</li>
 *   <li>{@link #MODE_PIN}            – user must enter a previously set 4-digit PIN</li>
 *   <li>{@link #MODE_MATH_CHALLENGE} – user must solve a random arithmetic problem</li>
 *   <li>{@link #MODE_DELAY}          – a countdown (default 30 s) must finish before
 *                                      the unlock button becomes tappable</li>
 * </ul>
 *
 * <p>State is persisted in "AddictionPrefs" under:</p>
 * <ul>
 *   <li>{@code strict_mode}      – int ordinal of the current mode</li>
 *   <li>{@code strict_pin}       – 4-digit PIN string</li>
 *   <li>{@code strict_delay_sec} – delay in seconds for DELAY mode (default 30)</li>
 * </ul>
 */
public class StrictLockModeManager {

    private static final String PREF_NAME = "AddictionPrefs";

    // Keys
    static final String KEY_MODE        = "strict_mode";
    static final String KEY_PIN         = "strict_pin";
    static final String KEY_DELAY_SEC   = "strict_delay_sec";

    public static final int MODE_NORMAL         = 0;
    public static final int MODE_PIN            = 1;
    public static final int MODE_MATH_CHALLENGE = 2;
    public static final int MODE_DELAY          = 3;

    public static final int DEFAULT_DELAY_SEC   = 30;
    public static final int DEFAULT_DELAY_MAX_SEC = 120;

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public static int getMode(Context context) {
        return prefs(context).getInt(KEY_MODE, MODE_NORMAL);
    }

    public static void setMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_MODE, mode).apply();
    }

    public static String getPin(Context context) {
        return prefs(context).getString(KEY_PIN, "");
    }

    public static void setPin(Context context, String pin) {
        prefs(context).edit().putString(KEY_PIN, pin).apply();
    }

    public static boolean hasPin(Context context) {
        String pin = getPin(context);
        return pin != null && pin.length() == 4;
    }

    public static int getDelaySec(Context context) {
        return prefs(context).getInt(KEY_DELAY_SEC, DEFAULT_DELAY_SEC);
    }

    public static void setDelaySec(Context context, int seconds) {
        int clamped = Math.max(5, Math.min(DEFAULT_DELAY_MAX_SEC, seconds));
        prefs(context).edit().putInt(KEY_DELAY_SEC, clamped).apply();
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    /**
     * Validates a PIN unlock attempt. Returns {@code true} if the supplied {@code input}
     * matches the stored PIN.
     */
    public static boolean validatePin(Context context, String input) {
        String stored = getPin(context);
        return stored != null && stored.equals(input);
    }

    // ─── Math Challenge ──────────────────────────────────────────────────────

    /**
     * Generates a math challenge question and returns it as a {@link MathChallenge}.
     * The answer is stored in the object; validate with {@link MathChallenge#check(String)}.
     */
    public static MathChallenge generateMathChallenge() {
        Random rng = new Random();
        int level = rng.nextInt(3); // 0 = easy, 1 = medium, 2 = hard

        int a, b, answer;
        String question;

        switch (level) {
            case 1: {
                // Medium: multiplication
                a = rng.nextInt(10) + 2;
                b = rng.nextInt(10) + 2;
                answer = a * b;
                question = a + " × " + b + " = ?";
                break;
            }
            case 2: {
                // Hard: two-step expression  a * b + c
                a = rng.nextInt(9) + 2;
                b = rng.nextInt(9) + 2;
                int c = rng.nextInt(50) + 1;
                answer = a * b + c;
                question = a + " × " + b + " + " + c + " = ?";
                break;
            }
            default: {
                // Easy: addition
                a = rng.nextInt(50) + 10;
                b = rng.nextInt(50) + 10;
                answer = a + b;
                question = a + " + " + b + " = ?";
            }
        }

        return new MathChallenge(question, answer);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─── Data model ───────────────────────────────────────────────────────────

    /** Immutable representation of a generated math challenge. */
    public static class MathChallenge {
        public final String question;
        private final int answer;

        MathChallenge(String question, int answer) {
            this.question = question;
            this.answer = answer;
        }

        /** Returns true when the user's input (trimmed) equals the correct answer. */
        public boolean check(String userInput) {
            if (userInput == null) return false;
            try {
                return Integer.parseInt(userInput.trim()) == answer;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}

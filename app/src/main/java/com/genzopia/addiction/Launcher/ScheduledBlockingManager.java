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
 * Manages scheduled blocking windows — time ranges during which the app-blocking
 * mode activates automatically (e.g. "every weekday 9 AM – 12 PM").
 *
 * <p>Schedules are persisted as a JSON list of {@link ScheduleEntry} under the key
 * {@code schedule_entries} in "AddictionPrefs".</p>
 *
 * <p>Call {@link #shouldBeActiveNow(Context)} from any periodic check (e.g. the
 * notification countdown, or a WorkManager job) to determine whether a schedule
 * window covers the current moment. The caller is responsible for then activating
 * the focus lock via {@link SharedPrefHelper}.</p>
 */
public class ScheduledBlockingManager {

    private static final String PREF_NAME     = "AddictionPrefs";
    static final String KEY_SCHEDULES = "schedule_entries";

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    public static List<ScheduleEntry> getSchedules(Context context) {
        String json = prefs(context).getString(KEY_SCHEDULES, null);
        if (json == null) return new ArrayList<>();
        try {
            Type type = new TypeToken<ArrayList<ScheduleEntry>>() {}.getType();
            List<ScheduleEntry> list = new Gson().fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveSchedules(Context context, List<ScheduleEntry> schedules) {
        prefs(context).edit()
                .putString(KEY_SCHEDULES, new Gson().toJson(schedules))
                .apply();
    }

    public static void addSchedule(Context context, ScheduleEntry entry) {
        List<ScheduleEntry> list = getSchedules(context);
        list.add(entry);
        saveSchedules(context, list);
    }

    public static void removeSchedule(Context context, int index) {
        List<ScheduleEntry> list = getSchedules(context);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            saveSchedules(context, list);
        }
    }

    public static void toggleSchedule(Context context, int index, boolean enabled) {
        List<ScheduleEntry> list = getSchedules(context);
        if (index >= 0 && index < list.size()) {
            list.get(index).enabled = enabled;
            saveSchedules(context, list);
        }
    }

    // ─── Activation check ────────────────────────────────────────────────────

    /**
     * Returns {@code true} when at least one enabled schedule covers the current
     * moment (day-of-week + hour). Call this whenever you need to decide whether
     * to auto-start a focus session.
     */
    public static boolean shouldBeActiveNow(Context context) {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // Calendar.MONDAY = 2 … SUNDAY = 1
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int currentMinutes = hour * 60 + minute;

        for (ScheduleEntry entry : getSchedules(context)) {
            if (!entry.enabled) continue;
            if (!entry.activeDays.contains(dayOfWeek)) continue;

            int startMinutes = entry.startHour * 60 + entry.startMinute;
            int endMinutes = entry.endHour * 60 + entry.endMinute;

            if (currentMinutes >= startMinutes && currentMinutes < endMinutes) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the {@link ScheduleEntry} that is currently active, or {@code null}
     * if none is active right now.
     */
    public static ScheduleEntry getActiveSchedule(Context context) {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int currentMinutes = hour * 60 + minute;

        for (ScheduleEntry entry : getSchedules(context)) {
            if (!entry.enabled) continue;
            if (!entry.activeDays.contains(dayOfWeek)) continue;
            int start = entry.startHour * 60 + entry.startMinute;
            int end   = entry.endHour * 60 + entry.endMinute;
            if (currentMinutes >= start && currentMinutes < end) return entry;
        }
        return null;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─── Data model ───────────────────────────────────────────────────────────

    /**
     * Represents one recurring blocking window.
     * Stored as plain fields so Gson can serialize/deserialize without adapters.
     */
    public static class ScheduleEntry {
        /** Human-readable label, e.g. "Deep Work Morning". */
        public String label;
        /** {@link Calendar} day constants that this schedule is active on (1=Sun … 7=Sat). */
        public List<Integer> activeDays;
        public int startHour;
        public int startMinute;
        public int endHour;
        public int endMinute;
        /** Duration in minutes that the focus lock should run for when triggered. */
        public int durationMinutes;
        /** Whether this schedule is currently active. */
        public boolean enabled;

        public ScheduleEntry() {
            activeDays = new ArrayList<>();
            enabled = true;
        }

        public ScheduleEntry(String label, List<Integer> days,
                             int startHour, int startMinute,
                             int endHour, int endMinute,
                             int durationMinutes) {
            this.label = label;
            this.activeDays = days;
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.endHour = endHour;
            this.endMinute = endMinute;
            this.durationMinutes = durationMinutes;
            this.enabled = true;
        }

        /** E.g. "09:00 – 12:00". */
        public String getTimeRangeLabel() {
            return String.format("%02d:%02d – %02d:%02d",
                    startHour, startMinute, endHour, endMinute);
        }

        /** E.g. "Mon, Tue, Wed". */
        public String getDaysLabel() {
            String[] names = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            StringBuilder sb = new StringBuilder();
            for (int day : activeDays) {
                if (sb.length() > 0) sb.append(", ");
                if (day >= 1 && day <= 7) sb.append(names[day]);
            }
            return sb.toString();
        }
    }
}

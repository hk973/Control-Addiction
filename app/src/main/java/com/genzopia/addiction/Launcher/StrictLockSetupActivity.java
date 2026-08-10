package com.genzopia.addiction.Launcher;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.genzopia.addiction.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration screen for Strict Lock Mode and Scheduled Blocking.
 * Reached via Settings → "Strict Lock Setup".
 */
public class StrictLockSetupActivity extends AppCompatActivity {

    private ScheduleAdapter scheduleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_strict_lock_setup);

        setupToolbar();
        setupLockModePicker();
        setupScheduleSection();
    }

    // ─── Toolbar ─────────────────────────────────────────────────────────────

    private void setupToolbar() {
        findViewById(R.id.lock_setup_back).setOnClickListener(v -> finish());
    }

    // ─── Lock Mode ───────────────────────────────────────────────────────────

    private void setupLockModePicker() {
        RadioGroup group = findViewById(R.id.lock_mode_group);
        View pinSection   = findViewById(R.id.pin_section);
        View delaySection = findViewById(R.id.delay_section);

        // Restore saved selection
        int currentMode = StrictLockModeManager.getMode(this);
        switch (currentMode) {
            case StrictLockModeManager.MODE_PIN:
                group.check(R.id.mode_pin);
                pinSection.setVisibility(View.VISIBLE);
                break;
            case StrictLockModeManager.MODE_MATH_CHALLENGE:
                group.check(R.id.mode_math);
                break;
            case StrictLockModeManager.MODE_DELAY:
                group.check(R.id.mode_delay);
                delaySection.setVisibility(View.VISIBLE);
                break;
            default:
                group.check(R.id.mode_normal);
        }

        group.setOnCheckedChangeListener((g, checkedId) -> {
            pinSection.setVisibility(View.GONE);
            delaySection.setVisibility(View.GONE);

            if (checkedId == R.id.mode_normal) {
                StrictLockModeManager.setMode(this, StrictLockModeManager.MODE_NORMAL);
            } else if (checkedId == R.id.mode_pin) {
                StrictLockModeManager.setMode(this, StrictLockModeManager.MODE_PIN);
                pinSection.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.mode_math) {
                StrictLockModeManager.setMode(this, StrictLockModeManager.MODE_MATH_CHALLENGE);
                Toast.makeText(this, "Math challenge mode enabled!", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.mode_delay) {
                StrictLockModeManager.setMode(this, StrictLockModeManager.MODE_DELAY);
                delaySection.setVisibility(View.VISIBLE);
            }
        });

        // PIN save button
        EditText pinInput = findViewById(R.id.pin_input);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        String existingPin = StrictLockModeManager.getPin(this);
        if (!existingPin.isEmpty()) {
            pinInput.setHint("PIN saved ✓ (" + existingPin.length() + " digits)");
        }

        findViewById(R.id.save_pin_btn).setOnClickListener(v -> {
            String pin = pinInput.getText().toString().trim();
            if (pin.length() != 4) {
                Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show();
            } else {
                StrictLockModeManager.setPin(this, pin);
                Toast.makeText(this, "PIN saved!", Toast.LENGTH_SHORT).show();
                pinInput.setText("");
                pinInput.setHint("PIN saved ✓");
            }
        });

        // Delay seekbar — range 5–120 seconds (progress 0–115 maps to 5–120)
        SeekBar seekBar  = findViewById(R.id.delay_seekbar);
        TextView delayTv = findViewById(R.id.delay_value_tv);
        int savedDelay   = StrictLockModeManager.getDelaySec(this);
        seekBar.setProgress(Math.max(0, savedDelay - 5));
        delayTv.setText(savedDelay + " seconds");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int secs = progress + 5;
                delayTv.setText(secs + " seconds");
                if (fromUser) StrictLockModeManager.setDelaySec(StrictLockSetupActivity.this, secs);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    // ─── Schedule Section ────────────────────────────────────────────────────

    private void setupScheduleSection() {
        RecyclerView rv        = findViewById(R.id.schedule_rv);
        TextView noSchedulesTv = findViewById(R.id.no_schedules_tv);

        List<ScheduledBlockingManager.ScheduleEntry> schedules =
                ScheduledBlockingManager.getSchedules(this);

        scheduleAdapter = new ScheduleAdapter(schedules, noSchedulesTv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(scheduleAdapter);

        updateEmptyState(noSchedulesTv, schedules.isEmpty());

        findViewById(R.id.add_schedule_btn).setOnClickListener(v -> showAddScheduleDialog());
    }

    private void updateEmptyState(TextView tv, boolean empty) {
        tv.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showAddScheduleDialog() {
        final String[] dayNames     = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        final boolean[] selectedDays = new boolean[7];

        final int[] startHour = {9};
        final int[] startMin  = {0};
        final int[] endHour   = {12};
        final int[] endMin    = {0};

        View dialogView   = LayoutInflater.from(this).inflate(R.layout.dialog_add_schedule, null, false);
        EditText labelIn  = dialogView.findViewById(R.id.schedule_label_input);
        TextView daysTv   = dialogView.findViewById(R.id.schedule_days_selected);
        TextView startTv  = dialogView.findViewById(R.id.schedule_start_time);
        TextView endTv    = dialogView.findViewById(R.id.schedule_end_time);
        EditText durIn    = dialogView.findViewById(R.id.schedule_duration_input);

        startTv.setText(String.format("%02d:%02d", startHour[0], startMin[0]));
        endTv.setText(String.format("%02d:%02d", endHour[0], endMin[0]));

        daysTv.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Select Days")
                        .setMultiChoiceItems(dayNames, selectedDays,
                                (d, which, isChecked) -> selectedDays[which] = isChecked)
                        .setPositiveButton("OK", (d, w) -> {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < dayNames.length; i++) {
                                if (selectedDays[i]) {
                                    if (sb.length() > 0) sb.append(", ");
                                    sb.append(dayNames[i]);
                                }
                            }
                            daysTv.setText(sb.length() > 0 ? sb.toString() : "None selected");
                        })
                        .show()
        );

        startTv.setOnClickListener(v -> new TimePickerDialog(this,
                (tp, h, m) -> { startHour[0] = h; startMin[0] = m;
                    startTv.setText(String.format("%02d:%02d", h, m)); },
                startHour[0], startMin[0], true).show());

        endTv.setOnClickListener(v -> new TimePickerDialog(this,
                (tp, h, m) -> { endHour[0] = h; endMin[0] = m;
                    endTv.setText(String.format("%02d:%02d", h, m)); },
                endHour[0], endMin[0], true).show());

        new AlertDialog.Builder(this)
                .setTitle("Add Schedule")
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    String label = labelIn.getText().toString().trim();
                    if (label.isEmpty()) label = "Focus Block";

                    List<Integer> days = new ArrayList<>();
                    for (int i = 0; i < selectedDays.length; i++) {
                        if (selectedDays[i]) days.add(i + 1); // Calendar: 1=Sun
                    }
                    if (days.isEmpty()) {
                        Toast.makeText(this, "Select at least one day", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int duration = 60;
                    try {
                        duration = Integer.parseInt(durIn.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}

                    ScheduledBlockingManager.addSchedule(this,
                            new ScheduledBlockingManager.ScheduleEntry(
                                    label, days,
                                    startHour[0], startMin[0],
                                    endHour[0], endMin[0], duration));

                    scheduleAdapter.reload(ScheduledBlockingManager.getSchedules(this));
                    updateEmptyState(findViewById(R.id.no_schedules_tv), false);
                    Toast.makeText(this, "Schedule saved!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Inner adapter ───────────────────────────────────────────────────────

    private class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {
        private List<ScheduledBlockingManager.ScheduleEntry> data;
        private final TextView emptyView;

        ScheduleAdapter(List<ScheduledBlockingManager.ScheduleEntry> data, TextView emptyView) {
            this.data = data;
            this.emptyView = emptyView;
        }

        void reload(List<ScheduledBlockingManager.ScheduleEntry> newData) {
            this.data = newData;
            notifyDataSetChanged();
            emptyView.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_schedule, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ScheduledBlockingManager.ScheduleEntry entry = data.get(pos);
            h.label.setText(entry.label);
            h.days.setText(entry.getDaysLabel());
            h.time.setText(entry.getTimeRangeLabel());
            h.toggle.setChecked(entry.enabled);

            h.toggle.setOnCheckedChangeListener((btn, checked) -> {
                int idx = h.getAdapterPosition();
                if (idx != RecyclerView.NO_ID) {
                    ScheduledBlockingManager.toggleSchedule(
                            StrictLockSetupActivity.this, idx, checked);
                }
            });

            h.delete.setOnClickListener(v -> {
                int idx = h.getAdapterPosition();
                if (idx != RecyclerView.NO_ID) {
                    ScheduledBlockingManager.removeSchedule(StrictLockSetupActivity.this, idx);
                    reload(ScheduledBlockingManager.getSchedules(StrictLockSetupActivity.this));
                }
            });
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView label, days, time;
            SwitchMaterial toggle;
            View delete;
            VH(@NonNull View v) {
                super(v);
                label  = v.findViewById(R.id.schedule_label);
                days   = v.findViewById(R.id.schedule_days);
                time   = v.findViewById(R.id.schedule_time);
                toggle = v.findViewById(R.id.schedule_toggle);
                delete = v.findViewById(R.id.schedule_delete);
            }
        }
    }
}

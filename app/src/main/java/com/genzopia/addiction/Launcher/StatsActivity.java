package com.genzopia.addiction.Launcher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.genzopia.addiction.R;
import com.genzopia.addiction.data.AppUsageTracker;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Stats Dashboard — shows:
 * 1. Level / XP progress bar
 * 2. Streak, sessions, total XP counters
 * 3. Earned badges grid
 * 4. Per-app screen-time list for today (requires PACKAGE_USAGE_STATS)
 */
public class StatsActivity extends AppCompatActivity {

    private ProgressBar xpProgressBar;
    private TextView levelTitleTv, xpLabelTv, streakValueTv, sessionsValueTv, xpValueTv;
    private RecyclerView badgesRv, usageRv;
    private View permissionBanner;
    private TextView noUsageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        bindViews();
        setupToolbar();
        loadGamificationData();
        loadUsageData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-load in case user just granted usage permission
        loadUsageData();
    }

    // ─── Binding ─────────────────────────────────────────────────────────────

    private void bindViews() {
        xpProgressBar   = findViewById(R.id.stats_xp_progress);
        levelTitleTv    = findViewById(R.id.stats_level_title);
        xpLabelTv       = findViewById(R.id.stats_xp_label);
        streakValueTv   = findViewById(R.id.stats_streak_value);
        sessionsValueTv = findViewById(R.id.stats_sessions_value);
        xpValueTv       = findViewById(R.id.stats_xp_value);
        badgesRv        = findViewById(R.id.stats_badges_rv);
        usageRv         = findViewById(R.id.stats_usage_rv);
        permissionBanner= findViewById(R.id.stats_usage_permission_banner);
        noUsageText     = findViewById(R.id.stats_no_usage_text);
    }

    private void setupToolbar() {
        findViewById(R.id.stats_back_btn).setOnClickListener(v -> finish());

        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navSettings = findViewById(R.id.nav_settings);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainContainerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }
    }

    // ─── Gamification ────────────────────────────────────────────────────────

    private void loadGamificationData() {
        int level    = GamificationManager.getLevel(this);
        long xp      = GamificationManager.getXP(this);
        int streak   = GamificationManager.getStreak(this);
        int sessions = GamificationManager.getSessions(this);
        int progress = GamificationManager.getLevelProgress(this);
        String title = GamificationManager.getLevelTitle(this);
        long xpForNext = GamificationManager.xpRequiredForLevel(level + 1);

        levelTitleTv.setText("Level " + level + " — " + title);
        xpLabelTv.setText(xp + " / " + xpForNext + " XP");
        xpProgressBar.setProgress(progress);
        streakValueTv.setText(String.valueOf(streak));
        sessionsValueTv.setText(String.valueOf(sessions));
        xpValueTv.setText(String.valueOf(xp));

        List<String> badges = GamificationManager.getEarnedBadges(this);
        badgesRv.setLayoutManager(new GridLayoutManager(this, 4));
        badgesRv.setAdapter(new BadgeAdapter(badges));
    }

    // ─── Usage data ──────────────────────────────────────────────────────────

    private void loadUsageData() {
        if (!AppUsageTracker.hasPermission(this)) {
            permissionBanner.setVisibility(View.VISIBLE);
            findViewById(R.id.stats_grant_usage_btn).setOnClickListener(v -> {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            });
            noUsageText.setVisibility(View.VISIBLE);
            usageRv.setVisibility(View.GONE);
            return;
        }

        permissionBanner.setVisibility(View.GONE);

        // Query on background thread — UsageStatsManager can hit disk
        Executors.newSingleThreadExecutor().execute(() -> {
            List<AppUsageTracker.AppUsageStat> stats = AppUsageTracker.getTodayUsage(this);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (stats.isEmpty()) {
                    noUsageText.setVisibility(View.VISIBLE);
                    usageRv.setVisibility(View.GONE);
                } else {
                    noUsageText.setVisibility(View.GONE);
                    usageRv.setVisibility(View.VISIBLE);
                    usageRv.setLayoutManager(new LinearLayoutManager(this));
                    usageRv.setAdapter(new UsageAdapter(stats));
                }
            });
        });
    }

    // ─── Adapters ────────────────────────────────────────────────────────────

    /** RecyclerView adapter for the badges grid. */
    private static class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.VH> {
        private final List<String> badges;

        BadgeAdapter(List<String> badges) { this.badges = badges; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_badge, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            String id = badges.get(pos);
            h.emoji.setText(GamificationManager.getBadgeEmoji(id));
            h.title.setText(GamificationManager.getBadgeTitle(id));
        }

        @Override public int getItemCount() { return badges.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView emoji, title;
            VH(@NonNull View v) {
                super(v);
                emoji = v.findViewById(R.id.badge_emoji);
                title = v.findViewById(R.id.badge_title);
            }
        }
    }

    /** RecyclerView adapter for the per-app usage list. */
    private class UsageAdapter extends RecyclerView.Adapter<UsageAdapter.VH> {
        private final List<AppUsageTracker.AppUsageStat> stats;
        private final long maxMs;

        UsageAdapter(List<AppUsageTracker.AppUsageStat> stats) {
            this.stats = stats;
            this.maxMs = stats.isEmpty() ? 1 : stats.get(0).totalTimeMs;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_usage, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AppUsageTracker.AppUsageStat stat = stats.get(pos);
            h.appName.setText(stat.label);
            h.timeLabel.setText(stat.getFormattedTime());
            int progress = maxMs > 0 ? (int) ((stat.totalTimeMs * 100) / maxMs) : 0;
            h.progressBar.setProgress(progress);

            // Load icon off main thread
            new Thread(() -> {
                Drawable icon = null;
                try {
                    icon = getPackageManager().getApplicationIcon(stat.packageName);
                } catch (PackageManager.NameNotFoundException ignored) { }
                final Drawable finalIcon = icon;
                runOnUiThread(() -> {
                    if (finalIcon != null) h.icon.setImageDrawable(finalIcon);
                });
            }).start();
        }

        @Override public int getItemCount() { return stats.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView appName, timeLabel;
            ProgressBar progressBar;
            VH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.usage_app_icon);
                appName = v.findViewById(R.id.usage_app_name);
                timeLabel = v.findViewById(R.id.usage_time_label);
                progressBar = v.findViewById(R.id.usage_progress);
            }
        }
    }
}

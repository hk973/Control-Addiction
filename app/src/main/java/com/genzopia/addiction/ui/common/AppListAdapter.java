package com.genzopia.addiction.ui.common;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.genzopia.addiction.Launcher.FastScrollView;
import com.genzopia.addiction.Launcher.SharedPrefHelper;
import com.genzopia.addiction.R;
import com.genzopia.addiction.data.AppUsageTracker;
import com.genzopia.addiction.data.model.AppInfo;

import java.util.Map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single adapter used by every app list of the launcher. The concrete behaviour
 * (icons, pin indicator, selection, launch on click or long press) is described
 * by {@link Config}.
 */
public class AppListAdapter extends ListAdapter<AppInfo, AppListAdapter.AppViewHolder>
        implements Filterable {

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    public interface OnAppLongClickListener {
        boolean onAppLongClick(AppInfo app);
    }

    /** Description of one list use-case. */
    public static class Config {
        boolean showIcon;
        boolean showPin;
        boolean selectable;
        boolean launchOnClick;
        boolean clickToOpenAware;
        List<String> selection = new ArrayList<>();
        OnAppClickListener clickListener;
        OnAppLongClickListener longClickListener;

        public Config showIcon(boolean value) {
            this.showIcon = value;
            return this;
        }

        public Config showPin(boolean value) {
            this.showPin = value;
            return this;
        }

        /** Highlights and toggles the packages of the given (externally owned) list. */
        public Config selectable(List<String> selection) {
            this.selectable = true;
            this.selection = selection;
            return this;
        }

        /** The adapter launches the app itself when the row is clicked. */
        public Config launchOnClick(boolean value) {
            this.launchOnClick = value;
            return this;
        }

        /** Launching follows SharedPrefHelper.isClickToOpen(): click when true, long press otherwise. */
        public Config clickToOpenAware(boolean value) {
            this.clickToOpenAware = value;
            return this;
        }

        public Config onClick(OnAppClickListener listener) {
            this.clickListener = listener;
            return this;
        }

        public Config onLongClick(OnAppLongClickListener listener) {
            this.longClickListener = listener;
            return this;
        }
    }

    private static final DiffUtil.ItemCallback<AppInfo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AppInfo>() {
                @Override
                public boolean areItemsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
                    return oldItem.getPackageName().equals(newItem.getPackageName());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
                    return oldItem.getLabel().equals(newItem.getLabel());
                }
            };

    private final Config config;
    private final SharedPrefHelper sharedPrefHelper;
    private final List<AppInfo> fullList = new ArrayList<>();
    private final List<String> pinnedApps = new ArrayList<>();
    private List<FastScrollView.Section> sections = new ArrayList<>();
    private CharSequence currentQuery = "";
    private int pinnedCount = 0;
    private Runnable listCommittedListener;
    /** Optional cache of {packageName → minutesUsedToday}. Populated by MainFragment. */
    private Map<String, Long> usageCache = Collections.emptyMap();

    /** Provide today's usage map so each row can show a time badge. */
    public void setUsageCache(Map<String, Long> cache) {
        this.usageCache = cache != null ? cache : Collections.emptyMap();
        if (getItemCount() > 0) notifyItemRangeChanged(0, getItemCount());
    }

    public AppListAdapter(Context context, Config config) {
        super(DIFF_CALLBACK);
        this.config = config;
        // Application context only: the adapter outlives nothing, but must not pin an Activity.
        this.sharedPrefHelper = new SharedPrefHelper(context.getApplicationContext());
    }

    /** Replaces the backing data; the current search query stays applied. */
    public void submitAppList(List<AppInfo> apps) {
        fullList.clear();
        if (apps != null) fullList.addAll(apps);
        applyFilter(currentQuery);
    }

    public List<AppInfo> getFullList() {
        return new ArrayList<>(fullList);
    }

    public void setPinnedApps(List<String> packageNames) {
        pinnedApps.clear();
        if (packageNames != null) pinnedApps.addAll(packageNames);
        if (getItemCount() > 0) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public void setPinnedCount(int pinnedCount) {
        this.pinnedCount = pinnedCount;
        sections = computeSections(getCurrentList(), pinnedCount);
        notifyListCommitted();
    }

    public List<FastScrollView.Section> getSections() {
        return sections;
    }

    /** Invoked after every committed list change, once the sections are up to date. */
    public void setOnListCommittedListener(Runnable listener) {
        this.listCommittedListener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_row, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = getItem(position);
        Context context = holder.itemView.getContext();
        holder.appName.setText(app.getLabel());

        if (config.showIcon) {
            holder.appIcon.setVisibility(View.VISIBLE);
            holder.appIcon.setImageDrawable(app.getIcon());
        } else {
            holder.appIcon.setVisibility(View.GONE);
        }

        boolean pinned = config.showPin && pinnedApps.contains(app.getPackageName());
        holder.pinIcon.setVisibility(pinned ? View.VISIBLE : View.GONE);

        // Show usage time badge if data is available
        if (holder.usageTime != null) {
            Long minutes = usageCache.get(app.getPackageName());
            if (minutes != null && minutes > 0) {
                String label = minutes >= 60
                        ? (minutes / 60) + "h " + (minutes % 60) + "m"
                        : minutes + "m";
                holder.usageTime.setText(label);
                holder.usageTime.setVisibility(View.VISIBLE);
            } else {
                holder.usageTime.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);

        boolean clickToOpen = sharedPrefHelper.isClickToOpen();

        if (config.selectable) {
            holder.itemView.setBackgroundColor(config.selection.contains(app.getPackageName())
                    ? selectedColor(context) : unselectedColor(context));
        }

        if (config.clickListener != null) {
            holder.itemView.setOnClickListener(v -> config.clickListener.onAppClick(app));
        } else if (config.launchOnClick && (!config.clickToOpenAware || clickToOpen)) {
            holder.itemView.setOnClickListener(v -> launchApp(context, app.getPackageName()));
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (pinned && config.longClickListener != null) {
                return config.longClickListener.onAppLongClick(app);
            }
            if (config.selectable && clickToOpen) {
                toggleSelection(app.getPackageName(), holder.getBindingAdapterPosition());
                return true;
            }
            if (config.launchOnClick && (!config.clickToOpenAware || !clickToOpen)) {
                launchApp(context, app.getPackageName());
                return true;
            }
            return config.longClickListener != null && config.longClickListener.onAppLongClick(app);
        });
    }

    private void toggleSelection(String packageName, int position) {
        if (config.selection.contains(packageName)) {
            config.selection.remove(packageName);
        } else {
            config.selection.add(packageName);
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    private int selectedColor(Context context) {
        return ContextCompat.getColor(context, R.color.background_selected);
    }

    private int unselectedColor(Context context) {
        return ContextCompat.getColor(context, isDarkMode(context)
                ? R.color.background_unselected_dark : R.color.background_unselected_light);
    }

    private boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private void launchApp(Context context, String packageName) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            context.startActivity(launchIntent);
        } else {
            Toast.makeText(context, "Unable to open app", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Filter getFilter() {
        return appFilter;
    }

    private void applyFilter(CharSequence constraint) {
        appFilter.filter(constraint);
    }

    private final Filter appFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            results.values = filterApps(fullList, constraint, pinnedApps);
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            currentQuery = constraint == null ? "" : constraint;
            List<AppInfo> filtered = (List<AppInfo>) results.values;
            submitList(filtered, () -> {
                sections = computeSections(getCurrentList(), pinnedCount);
                notifyListCommitted();
            });
        }
    };

    private void notifyListCommitted() {
        if (listCommittedListener != null) {
            listCommittedListener.run();
        }
    }

    /** A–Z sections of the visible list, skipping the leading pinned entries. */
    public static List<FastScrollView.Section> computeSections(List<AppInfo> apps, int pinnedCount) {
        List<FastScrollView.Section> sections = new ArrayList<>();
        if (apps == null) return sections;

        String currentSection = null;
        for (int i = Math.max(0, pinnedCount); i < apps.size(); i++) {
            String label = apps.get(i).getLabel();
            if (TextUtils.isEmpty(label)) continue;

            String firstLetter = label.substring(0, 1).toUpperCase();
            if (!firstLetter.equals(currentSection)) {
                currentSection = firstLetter;
                sections.add(new FastScrollView.Section(currentSection, i));
            }
        }
        return sections;
    }

    /** Filters by label, keeping the pinned matches on top of the alphabetical rest. */
    public static List<AppInfo> filterApps(List<AppInfo> source, CharSequence constraint,
                                           List<String> pinnedApps) {
        List<AppInfo> filtered = new ArrayList<>();
        if (source == null) return filtered;

        if (TextUtils.isEmpty(constraint)) {
            filtered.addAll(source);
            return filtered;
        }

        String pattern = constraint.toString().toLowerCase().trim();
        List<AppInfo> pinned = new ArrayList<>();
        List<AppInfo> others = new ArrayList<>();

        for (AppInfo app : source) {
            if (app.getLabel() == null || !app.getLabel().toLowerCase().contains(pattern)) continue;
            if (pinnedApps != null && pinnedApps.contains(app.getPackageName())) {
                pinned.add(app);
            } else {
                others.add(app);
            }
        }
        Collections.sort(others, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));

        filtered.addAll(pinned);
        filtered.addAll(others);
        return filtered;
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final ImageView appIcon;
        final TextView appName;
        final ImageView pinIcon;
        final TextView usageTime;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon   = itemView.findViewById(R.id.app_icon);
            appName   = itemView.findViewById(R.id.app_name);
            pinIcon   = itemView.findViewById(R.id.pin_icon);
            usageTime = itemView.findViewById(R.id.app_usage_time); // may be null in older layouts

            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            pinIcon.setColorFilter(new ColorMatrixColorFilter(matrix));
        }
    }
}

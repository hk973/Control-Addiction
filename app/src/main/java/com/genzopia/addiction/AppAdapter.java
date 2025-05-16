package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> implements Filterable {

     List<AppItem_Dataclass> appList;
     List<AppItem_Dataclass> appListFull;
    private ArrayList<String> selectedApps;
    private SharedPrefHelper sharedPrefHelper;
    private int pinnedCount = 0;
    private List<String> pinnedApps = new ArrayList<>();
    private List<FastScrollView.Section> sections = new ArrayList<>();
    Context con;

    public AppAdapter(List<AppItem_Dataclass> appList, ArrayList<String> selectedApps, Context context) {
        this.appList = appList;
        this.appListFull = new ArrayList<>(appList);
        this.selectedApps = selectedApps;
        this.sharedPrefHelper = new SharedPrefHelper(context);
        processSections();
        this.con=context;
    }

    private void processSections() {
        sections.clear();
        String currentSection = null;

        // Start from pinnedCount to skip pinned apps
        for (int i = pinnedCount; i < appList.size(); i++) {
            AppItem_Dataclass app = appList.get(i);
            String firstLetter = app.getName().substring(0, 1).toUpperCase();

            if (!firstLetter.equals(currentSection)) {
                currentSection = firstLetter;
                sections.add(new FastScrollView.Section(currentSection, i));
            }
        }
    }
    public List<FastScrollView.Section> getSections() {
        return sections;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }
    public void setPinnedCount(int pinnedCount) {
        this.pinnedCount = pinnedCount;
        processSections(); // Re-process sections with new pinned count
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem_Dataclass appItem = appList.get(position);
        holder.textView.setText(appItem.getName());
        // Show pin icon if app is pinned
        holder.pinIcon.setVisibility(pinnedApps.contains(appItem.getPackageName()) ? View.VISIBLE : View.GONE);

        int selectedColor = holder.itemView.getContext().getResources().getColor(R.color.background_selected);
        int unselectedColor = holder.itemView.getContext().getResources().getColor(
                isDarkMode(holder.itemView.getContext()) ? R.color.background_unselected_dark : R.color.background_unselected_light
        );

        holder.itemView.setBackgroundColor(selectedApps.contains(appItem.getPackageName()) ? selectedColor : unselectedColor);

        // Clear previous listeners
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);

        boolean isPinned = pinnedApps.contains(appItem.getPackageName());

        holder.itemView.setOnClickListener(v -> {
            launchApp(holder.itemView.getContext(), appItem.getPackageName());
        });




        holder.itemView.setOnLongClickListener(v -> {
            if (isPinned) {
                if (con instanceof MainContainerActivity) {
                    ((MainContainerActivity) con).showPinnedAppOptions(appItem);
                }
                return true;
            } else {
                // Original long click behavior for non-pinned apps
                boolean isClickToOpen = sharedPrefHelper.isClickToOpen();
                if(isClickToOpen){
                    toggleSelection(holder.itemView, appItem.getPackageName(),
                            selectedColor, unselectedColor);
                } else {
                    launchApp(holder.itemView.getContext(), appItem.getPackageName());
                }
                return true;
            }
        });



    }


    // Update setPinnedApps
    public void setPinnedApps(List<String> pinnedPackageNames) {
        this.pinnedApps = new ArrayList<>(pinnedPackageNames);
        notifyDataSetChanged();
    }


    private void toggleSelection(View itemView, String packageName, int selectedColor, int unselectedColor) {
        if (selectedApps.contains(packageName)) {
            selectedApps.remove(packageName);
            itemView.setBackgroundColor(unselectedColor);
        } else {
            selectedApps.add(packageName);
            itemView.setBackgroundColor(selectedColor);
        }
    }

    private void launchApp(Context context, String packageName) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            context.startActivity(launchIntent);
        } else {
            Toast.makeText(context, "Unable to open app", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }


    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView pinIcon;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.app_name);
            pinIcon = itemView.findViewById(R.id.pin_icon);
        }
    }

    // Implement the getFilter method for search functionality
    @Override
    public Filter getFilter() {
        return appFilter;
    }
    // Modify the filter to handle pinned apps correctly
    private final Filter appFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<AppItem_Dataclass> filtered = new ArrayList<>();
            List<AppItem_Dataclass> original = new ArrayList<>(appListFull);

            if (TextUtils.isEmpty(constraint)) {
                filtered.addAll(original);
            } else {
                String pattern = constraint.toString().toLowerCase().trim();

                // Separate pinned and others
                List<AppItem_Dataclass> pinned = new ArrayList<>();
                List<AppItem_Dataclass> others = new ArrayList<>();

                for (AppItem_Dataclass item : original) {
                    if (pinnedApps.contains(item.getPackageName())) {
                        pinned.add(item);
                    } else {
                        others.add(item);
                    }
                }

                // Filter pinned
                List<AppItem_Dataclass> filteredPinned = new ArrayList<>();
                for (AppItem_Dataclass item : pinned) {
                    if (item.getName().toLowerCase().contains(pattern)) {
                        filteredPinned.add(item);
                    }
                }

                // Filter others
                List<AppItem_Dataclass> filteredOthers = new ArrayList<>();
                for (AppItem_Dataclass item : others) {
                    if (item.getName().toLowerCase().contains(pattern)) {
                        filteredOthers.add(item);
                    }
                }
                Collections.sort(filteredOthers, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                filtered.addAll(filteredPinned);
                filtered.addAll(filteredOthers);
            }

            FilterResults results = new FilterResults();
            results.values = filtered;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            appList.clear();
            appList.addAll((List<AppItem_Dataclass>) results.values);
            processSections();
            notifyDataSetChanged();
        }

    };
    public void updateData(List<AppItem_Dataclass> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AppDiffCallback(this.appList, newData));
        this.appList = new ArrayList<>(newData);
        this.appListFull = new ArrayList<>(newData);
        diffResult.dispatchUpdatesTo(this);
        processSections();
    }


}

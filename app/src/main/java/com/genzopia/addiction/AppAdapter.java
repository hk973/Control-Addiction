package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> implements Filterable {

    private List<AppItem_Dataclass> appList;
    private List<AppItem_Dataclass> appListFull;
    private ArrayList<String> selectedApps;
    private SharedPrefHelper sharedPrefHelper;
    private List<FastScrollView.Section> sections = new ArrayList<>();

    public AppAdapter(List<AppItem_Dataclass> appList, ArrayList<String> selectedApps, Context context) {
        this.appList = appList;
        this.appListFull = new ArrayList<>(appList);
        this.selectedApps = selectedApps;
        this.sharedPrefHelper = new SharedPrefHelper(context);
        processSections();
    }
    private void processSections() {
        sections.clear();
        String currentSection = null;

        for (int i = 0; i < appList.size(); i++) {
            AppItem_Dataclass app = appList.get(i);
            String firstLetter = app.getName().substring(0, 1).toUpperCase();

            // Only add section if it's different from previous
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
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem_Dataclass appItem = appList.get(position);
        holder.textView.setText(appItem.getName());

        int selectedColor = holder.itemView.getContext().getResources().getColor(R.color.background_selected);
        int unselectedColor = holder.itemView.getContext().getResources().getColor(
                isDarkMode(holder.itemView.getContext()) ? R.color.background_unselected_dark : R.color.background_unselected_light
        );

        holder.itemView.setBackgroundColor(selectedApps.contains(appItem.getPackageName()) ? selectedColor : unselectedColor);

        // Clear previous listeners
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);


        holder.itemView.setOnClickListener(v -> {
            boolean isClickToOpen = sharedPrefHelper.isClickToOpen();
            if(isClickToOpen) {
                Log.e("test111", String.valueOf(isClickToOpen));
                launchApp(holder.itemView.getContext(), appItem.getPackageName());
            }else {
                toggleSelection(holder.itemView, appItem.getPackageName(), selectedColor, unselectedColor);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            boolean isClickToOpen = sharedPrefHelper.isClickToOpen();
            if(isClickToOpen){
                Log.e("test111", String.valueOf(isClickToOpen));
                toggleSelection(holder.itemView, appItem.getPackageName(), selectedColor, unselectedColor);
                return true;

            }else{
                Log.e("test111", String.valueOf(isClickToOpen));
                launchApp(holder.itemView.getContext(), appItem.getPackageName());
                return true;
            }

        });



    }
    public void updateData(List<AppItem_Dataclass> newData) {
        // Update both the main list and full list (for filtering)
        this.appList = new ArrayList<>(newData);
        this.appListFull = new ArrayList<>(newData);

        // Re-process sections for fast scroll
        processSections();

        // Update selected apps list to maintain selections
        ArrayList<String> newSelectedApps = new ArrayList<>();
        for (String packageName : selectedApps) {
            // Only keep selections that exist in the new data
            for (AppItem_Dataclass item : newData) {
                if (item.getPackageName().equals(packageName)) {
                    newSelectedApps.add(packageName);
                    break;
                }
            }
        }
        this.selectedApps = newSelectedApps;

        // Notify adapter of data change
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

    // View Holder class for RecyclerView
    public static class AppViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }

    // Implement the getFilter method for search functionality
    @Override
    public Filter getFilter() {
        return appFilter;
    }

    // Update filter to sort filtered results
    private final Filter appFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<AppItem_Dataclass> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(appListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (AppItem_Dataclass item : appListFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            // Sort filtered results
            Collections.sort(filteredList, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

            FilterResults results = new FilterResults();
            results.values = filteredList;
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
}
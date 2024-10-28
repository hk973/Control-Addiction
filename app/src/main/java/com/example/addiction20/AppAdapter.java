package com.example.addiction20;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> implements Filterable {

    private List<AppItem_Dataclass> appList;
    private List<AppItem_Dataclass> appListFull; // For storing the full list of apps for filtering
    private ArrayList<String> selectedApps;

    public AppAdapter(List<AppItem_Dataclass> appList, ArrayList<String> selectedApps) {
        this.appList = appList;
        this.appListFull = new ArrayList<>(appList); // Copy of full list for filtering
        this.selectedApps = selectedApps;
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

        // Change background color based on selection
        holder.itemView.setBackgroundColor(selectedApps.contains(appItem.getPackageName()) ? selectedColor : unselectedColor);

        // OnClickListener for selecting/deselecting the app
        holder.itemView.setOnClickListener(v -> {
            if (selectedApps.contains(appItem.getPackageName())) {
                selectedApps.remove(appItem.getPackageName());
                holder.itemView.setBackgroundColor(unselectedColor);
            } else {
                selectedApps.add(appItem.getPackageName());
                holder.itemView.setBackgroundColor(selectedColor);
            }
        });

        // OnLongClickListener to open the app
        holder.itemView.setOnLongClickListener(v -> {
            Context context = holder.itemView.getContext();
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appItem.getPackageName());
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            } else {
                Toast.makeText(context, "Unable to open app", Toast.LENGTH_SHORT).show();
            }
            return true; // Indicate that the long-click event is handled
        });
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

    private final Filter appFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<AppItem_Dataclass> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(appListFull); // Show all items if search is empty
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (AppItem_Dataclass item : appListFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item); // Add matching items to the filtered list
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            appList.clear();
            appList.addAll((List<AppItem_Dataclass>) results.values);
            notifyDataSetChanged();
        }
    };
}

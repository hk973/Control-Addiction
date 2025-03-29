package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SelectedAppsAdapter extends RecyclerView.Adapter<SelectedAppsAdapter.ViewHolder> {
    private final Context context;
    private ArrayList<String> selectedAppNames;
    private ArrayList<String> selectedAppPackages;
    private final ArrayList<String> fullAppNames;
    private final ArrayList<String> fullAppPackages;
    private final SharedPrefHelper sharedPrefHelper;

    public SelectedAppsAdapter(Context context, ArrayList<String> selectedAppNames, ArrayList<String> selectedAppPackages) {
        this.context = context;
        this.selectedAppNames = new ArrayList<>(selectedAppNames);
        this.fullAppNames = new ArrayList<>(selectedAppNames);
        this.selectedAppPackages = new ArrayList<>(selectedAppPackages);
        this.fullAppPackages = new ArrayList<>(selectedAppPackages);
        this.sharedPrefHelper = new SharedPrefHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String appName = selectedAppNames.get(position);
        String packageName = selectedAppPackages.get(position);
        holder.appNameTextView.setText(appName);

        // Clear previous listeners
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);

        boolean isClickToOpen = sharedPrefHelper.isClickToOpen();

        if (isClickToOpen) {
            // Click to open app
            holder.itemView.setOnClickListener(v -> launchApp(packageName));
        } else {
            // Long press to open app
            holder.itemView.setOnLongClickListener(v -> {
                launchApp(packageName);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return selectedAppNames.size();
    }

    private void launchApp(String packageName) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            context.startActivity(launchIntent);
        } else {
            Log.e("SelectedAppsAdapter", "Unable to launch app: " + packageName);
        }
    }

    // Filter method to update the RecyclerView based on search query
    public void filter(String query) {
        selectedAppNames.clear();
        selectedAppPackages.clear();
        if (query.isEmpty()) {
            selectedAppNames.addAll(fullAppNames); // Reset to full list
            selectedAppPackages.addAll(fullAppPackages); // Reset to full package list
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (int i = 0; i < fullAppNames.size(); i++) {
                String appName = fullAppNames.get(i);
                if (appName.toLowerCase().contains(lowerCaseQuery)) {
                    selectedAppNames.add(appName);
                    selectedAppPackages.add(fullAppPackages.get(i)); // Add corresponding package name
                }
            }
        }
        notifyDataSetChanged(); // Refresh the list
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appNameTextView;

        ViewHolder(View itemView) {
            super(itemView);
            appNameTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}

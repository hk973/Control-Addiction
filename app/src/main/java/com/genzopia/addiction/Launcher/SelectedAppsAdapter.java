// SelectedAppsAdapter.java
package com.genzopia.addiction.Launcher;

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
import java.util.List;

public class SelectedAppsAdapter extends RecyclerView.Adapter<SelectedAppsAdapter.ViewHolder> {
    private final Context context;
    private final List<String> fullAppNames;        // backup of the current list (all or selected)
    private final List<String> fullAppPackages;
    private List<String> displayAppNames;           // what’s actually shown
    private List<String> displayAppPackages;
    private final SharedPrefHelper sharedPrefHelper;

    public SelectedAppsAdapter(Context context,
                               List<String> initialNames,
                               List<String> initialPackages) {
        this.context = context;
        this.fullAppNames    = new ArrayList<>(initialNames);
        this.fullAppPackages = new ArrayList<>(initialPackages);
        this.displayAppNames    = new ArrayList<>(initialNames);
        this.displayAppPackages = new ArrayList<>(initialPackages);
        this.sharedPrefHelper = new SharedPrefHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1,
                        parent,
                        false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String appName     = displayAppNames.get(position);
        String packageName = displayAppPackages.get(position);
        holder.appNameTextView.setText(appName);

        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);

        boolean clickToOpen = sharedPrefHelper.isClickToOpen();
        if (clickToOpen) {
            holder.itemView.setOnClickListener(v -> launchApp(packageName));
        } else {
            holder.itemView.setOnLongClickListener(v -> {
                launchApp(packageName);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayAppNames.size();
    }

    private void launchApp(String pkg) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent != null) {
            context.startActivity(intent);
        } else {
            Log.e("Adapter", "Cannot launch: " + pkg);
        }
    }

    /** Replace the adapter’s data with a new set of names/packages */
    public void updateData(List<String> newNames, List<String> newPackages) {
        fullAppNames.clear();
        fullAppNames.addAll(newNames);
        fullAppPackages.clear();
        fullAppPackages.addAll(newPackages);

        displayAppNames.clear();
        displayAppNames.addAll(newNames);
        displayAppPackages.clear();
        displayAppPackages.addAll(newPackages);

        notifyDataSetChanged();
    }

    /** Filter current display list by a search query */
    public void filter(String query) {
        displayAppNames.clear();
        displayAppPackages.clear();

        if (query.isEmpty()) {
            displayAppNames.addAll(fullAppNames);
            displayAppPackages.addAll(fullAppPackages);
        } else {
            String q = query.toLowerCase();
            for (int i = 0; i < fullAppNames.size(); i++) {
                if (fullAppNames.get(i).toLowerCase().contains(q)) {
                    displayAppNames.add(fullAppNames.get(i));
                    displayAppPackages.add(fullAppPackages.get(i));
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appNameTextView;
        ViewHolder(View itemView) {
            super(itemView);
            appNameTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}

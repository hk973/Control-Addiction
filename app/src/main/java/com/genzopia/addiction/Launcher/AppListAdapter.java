package com.genzopia.addiction.Launcher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.genzopia.addiction.R;

import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private List<AppItem> appItems;
    private OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(AppItem appItem);
    }

    public AppListAdapter(List<AppItem> appItems, OnAppClickListener listener) {
        this.appItems = appItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_2, parent, false); // You'll need to create this layout
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem appItem = appItems.get(position);
        holder.appName.setText(appItem.getAppName());
        holder.appIcon.setImageDrawable(appItem.getIcon());

        holder.itemView.setOnClickListener(v -> listener.onAppClick(appItem));
    }

    @Override
    public int getItemCount() {
        return appItems.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
        }
    }
}

package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class RewardsAdapter extends RecyclerView.Adapter<RewardsAdapter.ViewHolder> {

    private final ArrayList<String> codeList;
    private final Context context;

    public RewardsAdapter(ArrayList<String> codeList, Context context) {
        this.codeList = codeList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reward_code, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String code = codeList.get(position);
        holder.codeTextView.setText(code);

        // Only make clickable if it's a real reward code
        boolean isClickable = !code.equals("No rewards available");
        holder.itemView.setClickable(isClickable);

        if (isClickable) {
            holder.itemView.setOnClickListener(v -> {
                // FIX: Remove delay to prevent context issues
                Intent intent = new Intent(context, ChallengeReward.class);
                intent.putExtra("REWARD_CODE", code);
                context.startActivity(intent);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return codeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView codeTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            codeTextView = itemView.findViewById(R.id.codeTextView);

            // FIX: Add null check for critical view
            if (codeTextView == null) {
                throw new IllegalStateException("TextView not found in item layout");
            }
        }
    }
}
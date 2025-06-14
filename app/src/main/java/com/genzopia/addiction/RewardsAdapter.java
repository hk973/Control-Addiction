package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class RewardsAdapter extends RecyclerView.Adapter<RewardsAdapter.ViewHolder> {

    private final ArrayList<String> codeList;
    private Context conn;

    public RewardsAdapter(ArrayList<String> codeList, Context con) {
        this.codeList = codeList;
        this.conn=con;
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

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(conn, ChallengeReward.class);
            intent.putExtra("REWARD_CODE", code);
            conn.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return codeList.size();
    }

    // ViewHolder Class
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView codeTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            codeTextView = itemView.findViewById(R.id.codeTextView);
        }
    }
}


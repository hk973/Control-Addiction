package com.genzopia.addiction;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.util.ArrayList;
import java.util.Collections;

public class Rewards_list extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rewards_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.rewardsRecyclerView);
        if (recyclerView == null) {
            Log.e("RewardsList", "RecyclerView not found!");
            return;
        }

        // Add item decoration for spacing
        recyclerView.addItemDecoration(new MaterialDividerItemDecoration(
                recyclerView.getContext(),
                DividerItemDecoration.VERTICAL
        ));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        SharedPrefHelper sp = new SharedPrefHelper(this);
        ArrayList<String> codeList = sp.get_challenge_code_list(this);

        // FIX: Handle null/empty list
        if (codeList == null || codeList.isEmpty()) {
            codeList = new ArrayList<>();
            codeList.add("No rewards available");
        }

        RewardsAdapter adapter = new RewardsAdapter(codeList, this);
        recyclerView.setAdapter(adapter);
    }
}
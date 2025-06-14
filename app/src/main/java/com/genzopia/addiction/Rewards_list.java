package com.genzopia.addiction;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

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

        // Initialize RecyclerView
        RecyclerView recyclerView = findViewById(R.id.rewardsRecyclerView);

        // Get codes from SharedPreferences
        SharedPrefHelper sp = new SharedPrefHelper(this);
        ArrayList<String> codeList = sp.get_challenge_code_list(this);

        // Set up adapter
        RewardsAdapter adapter = new RewardsAdapter(codeList,this);
        recyclerView.setAdapter(adapter);
    }

    // RecyclerView Adapter

}
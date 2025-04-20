package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

public class MainContainerActivity2 extends AppCompatActivity {
MainFragment mainFragment;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new MainPagerAdapter(this));
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {

        if (viewPager.getCurrentItem() == 1 ) {

                viewPager.setCurrentItem(0);

        }
    }
}
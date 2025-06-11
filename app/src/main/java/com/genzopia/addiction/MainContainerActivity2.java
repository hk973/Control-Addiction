package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainContainerActivity2 extends BaseActivity {
MainFragment mainFragment;
    private ViewPager2 viewPager;
    private MainPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        viewPager = findViewById(R.id.view_pager);
        adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);

    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {

        int currentItem = viewPager.getCurrentItem();
        Fragment fragment = adapter.getFragment(currentItem);

        if (fragment instanceof OnBack) {
            ((OnBack) fragment).back(viewPager); // call custom back

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

}
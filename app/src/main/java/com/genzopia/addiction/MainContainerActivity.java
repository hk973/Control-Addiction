package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class MainContainerActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private MainFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(1); // Pre-load adjacent pages
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        viewPager.setUserInputEnabled(true);

        // Get reference to MainFragment when it's created
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 1) {
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + position);
                    if (fragment instanceof MainFragment) {
                        mainFragment = (MainFragment) fragment;
                    }
                }
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 1 && mainFragment != null) {
            // If we're on MainFragment and time is set, reset it
            if (mainFragment.isTimeSet()) {
                mainFragment.resetTimeSelection();
                Toast.makeText(this, "Time selection reset", Toast.LENGTH_SHORT).show();
            } else {
                // If time isn't set, go back to home
                viewPager.setCurrentItem(0);
            }
        }
    }

    private static class ScreenSlidePagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public ScreenSlidePagerAdapter(MainContainerActivity fa) {
            super(fa);
        }

        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            return position == 0 ? new HomeFragment() : new MainFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
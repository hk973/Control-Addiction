package com.genzopia.addiction;



import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

public class MainContainerActivity extends AppCompatActivity {
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(1); // Pre-load adjacent pages
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        viewPager.setUserInputEnabled(true);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            viewPager.setCurrentItem(0);
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
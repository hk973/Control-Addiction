package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

public class MainContainerActivity extends AppCompatActivity {
    public ViewPager2 viewPager;
    private MainFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        viewPager.setUserInputEnabled(true);

        AppListViewModel viewModel = new ViewModelProvider(this).get(AppListViewModel.class);
        if (viewModel.getAppItemsLiveData().getValue() == null) {
            viewModel.loadApps(getApplicationContext());
        }

        SharedPrefHelper p = new SharedPrefHelper(this);
        if(p.getTimeLimitValue()<=0){
        p.saveTimeActivateStatus(false);}

        if (!p.getReviewShown()) {
            Intent reviewIntent = new Intent(this, ReviewActivity.class);
            startActivity(reviewIntent);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 1 && mainFragment != null) {
            if (mainFragment.isTimeSet()) {
                mainFragment.resetTimeSelection();
                Toast.makeText(this, "Time selection reset", Toast.LENGTH_SHORT).show();
            } else {
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
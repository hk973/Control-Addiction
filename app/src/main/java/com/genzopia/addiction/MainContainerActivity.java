package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

public class MainContainerActivity extends BaseActivity {
    public ViewPager2 viewPager;
    private MainFragment mainFragment;
    private AppUpdateChecker updateChecker;
    private static final int REQUEST_CODE = 123;

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

        updateChecker = new AppUpdateChecker(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(updateChecker);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            // Update cancelled or failed — optionally close app
            finish();
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
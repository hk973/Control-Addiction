// MainActivity.java
package com.genzopia.addiction.permission;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.genzopia.addiction.MainContainerActivity;
import com.genzopia.addiction.R;
import com.genzopia.addiction.SharedPrefHelper;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements PermissionListener {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private View[] dots;
    private PermissionPagerAdapter pagerAdapter;
    private int maxAllowedPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Check if we should skip to main activity
        SharedPrefHelper sp = new SharedPrefHelper(this);
        if (sp.getTimeActivateStatus()) {
            startMainContainerActivity();
            return;
        }
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#000000")); // Your color




        // Initialize ViewPager and dots
        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);

        setupViewPager();
        setupDots();

        // Listen for page changes to update dots
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                if (position > maxAllowedPosition) {
                    viewPager.setCurrentItem(maxAllowedPosition, false); // Block forward swipe
                } else {
                    updateDots(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private void setupViewPager() {
        pagerAdapter = new PermissionPagerAdapter(getSupportFragmentManager());

        // Add only the needed fragments (3 fragments now)
        pagerAdapter.addFragment(AccessibilityPermissionFragment.newInstance());
        pagerAdapter.addFragment(TermsFragment.newInstance());
        pagerAdapter.addFragment(ThemeSelectionFragment.newInstance());
        pagerAdapter.addFragment(LauncherPermissionFragment.newInstance());


        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(4); // Keep all fragments in memory
    }

    private void setupDots() {
        dots = new View[4]; // Only 3 dots now

        for (int i = 0; i <= 3; i++) {
            dots[i] = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.dot_width),
                    getResources().getDimensionPixelSize(R.dimen.dot_height)
            );
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);
            dots[i].setBackgroundResource(i == 0 ? R.drawable.dot_selected : R.drawable.dot_unselected);
            dotsLayout.addView(dots[i]);
        }
    }

    private void updateDots(int currentPosition) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(i == currentPosition ? R.drawable.dot_selected : R.drawable.dot_unselected);
        }
    }

    @Override
    public void onPermissionGranted(int position) {
        // Update max allowed position
        if (position + 1 > maxAllowedPosition) {
            maxAllowedPosition = position + 1;
        }

        // Move to next page if applicable
        if (position == viewPager.getCurrentItem() && position < pagerAdapter.getCount() - 1) {
            viewPager.postDelayed(() -> {
                try {
                    viewPager.setCurrentItem(position + 1, true);
                } catch (IllegalStateException e) {
                    // Handle exception gracefully
                    e.printStackTrace();
                }
            }, 100); // Small delay to ensure transaction safety
        }
    }

    @Override
    public void onAllPermissionsComplete() {
        // Start the main application
        startMainContainerActivity();
    }

    private void startMainContainerActivity() {
        SharedPrefHelper sp = new SharedPrefHelper(this);
        sp.setTimeActivateStatus(true);
        android.content.Intent intent = new android.content.Intent(this, MainContainerActivity.class);
        startActivity(intent);
        finish();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if(viewPager.getCurrentItem()==0){
        super.onBackPressed();}else{
        viewPager.setCurrentItem(0);}
    }

    /**
     * Adapter for the permission fragments
     */
    private class PermissionPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragmentList = new ArrayList<>();

        public PermissionPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void addFragment(Fragment fragment) {
            fragmentList.add(fragment);
        }
    }


}

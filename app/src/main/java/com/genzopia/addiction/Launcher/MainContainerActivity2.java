package com.genzopia.addiction.Launcher;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.genzopia.addiction.R;

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
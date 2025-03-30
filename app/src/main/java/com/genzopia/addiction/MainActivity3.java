package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MainActivity3 extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SelectedAppsAdapter adapter;
    private PackageManager packageManager;

    private long elapsedTime;
    private Handler handler = new Handler();
    private Runnable timerRunnable;
    private TextView timerTextView;
    private EditText searchBar;

    private ArrayList<String> appNames;
    private ArrayList<String> selectedApps;

    private boolean isTimerRunning = false;
    private SharedPrefHelper sharedPrefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);


        searchBar = findViewById(R.id.searchBar);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        packageManager = getPackageManager();

        sharedPrefHelper = new SharedPrefHelper(this);
        selectedApps = sharedPrefHelper.getSelectedAppValue();

        if (selectedApps != null && !selectedApps.isEmpty()) {
            appNames = (ArrayList<String>) getAppNamesFromPackageNames(selectedApps);
            adapter = new SelectedAppsAdapter(this, appNames, selectedApps);
            recyclerView.setAdapter(adapter);
        } else {
            Log.d("MainActivity3", "No selected apps found.");
        }

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        Intent serviceIntent = new Intent(this, ForegroundAppService.class);
        serviceIntent.putStringArrayListExtra("selectedApps", selectedApps);
        startService(serviceIntent);

    }
    private List<String> getAppNamesFromPackageNames(ArrayList<String> packageNames) {
        List<String> appNames = new ArrayList<>();
        for (String packageName : packageNames) {
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                appNames.add(appName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("MainActivity3", "Package not found: " + packageName);
            }
        }
        return appNames;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {}


    @Override
    protected void onResume() {
        Log.e("onresume","1");
        super.onResume();
        SharedPrefHelper sp=new SharedPrefHelper(this);
        boolean status=sp.getTimeActivateStatus();
        Log.e("status", String.valueOf(status));
        if(!status){
            Log.e("onresumestatus","1");
            Intent intent =new Intent(this, MainFragment.class);
            startActivity(intent);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("onpause","1");

        SharedPrefHelper sp=new SharedPrefHelper(this);
        boolean status=sp.getTimeActivateStatus();
        Log.e("status", String.valueOf(status));
        if(!status){
            Log.e("onpausestatus","1");
            Intent intent =new Intent(this, MainFragment.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, resid, first);
        Log.e("status","1");
    }
}

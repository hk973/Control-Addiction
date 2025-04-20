package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

public class SelectedAppsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SelectedAppsAdapter adapter;
    private PackageManager packageManager;
    private EditText searchBar;
    private ArrayList<String> appNames;
    private ArrayList<String> selectedApps;
    private SharedPrefHelper sharedPrefHelper;
    private Handler timeCheckHandler = new Handler();
    private static final long CHECK_INTERVAL = 1000;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BillingClient billingClient = BillingClient.newBuilder(requireContext())
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                        // Handle purchases
                    }
                })
                .enablePendingPurchases()
                .build();

        searchBar = view.findViewById(R.id.searchBar);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        packageManager = requireActivity().getPackageManager();
        sharedPrefHelper = new SharedPrefHelper(requireContext());
        selectedApps = sharedPrefHelper.getSelectedAppValue();

        if (selectedApps != null && !selectedApps.isEmpty()) {
            appNames = (ArrayList<String>) getAppNamesFromPackageNames(selectedApps);
            adapter = new SelectedAppsAdapter(requireContext(), appNames, selectedApps);
            recyclerView.setAdapter(adapter);
        }

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        Intent serviceIntent = new Intent(requireContext(), ForegroundAppService.class);
        serviceIntent.putStringArrayListExtra("selectedApps", selectedApps);
        requireContext().startService(serviceIntent);
    }
    private void startPeriodicTimeCheck() {
        timeCheckHandler.postDelayed(timeCheckRunnable, CHECK_INTERVAL);
    }
    private Runnable timeCheckRunnable = new Runnable() {
        @Override
        public void run() {
            SharedPrefHelper sp = new SharedPrefHelper(getContext());
            boolean status = sp.getTimeActivateStatus();

            if (!status) {
                // Time expired - redirect
                Intent intent = new Intent(getContext(), MainContainerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
























//                finish(); // Close current activity


            } else {
                // Continue checking
                timeCheckHandler.postDelayed(this, CHECK_INTERVAL);
            }
        }
    };
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
    public void onResume() {
        Log.e("onresume","1");
        super.onResume();
        SharedPrefHelper sp=new SharedPrefHelper(getContext());
        boolean status=sp.getTimeActivateStatus();
        Log.e("status", String.valueOf(status));
        if(!status){
            Log.e("onresumestatus","1");
            Intent intent =new Intent(getContext(), MainContainerActivity.class);
            startActivity(intent);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("onpause","1");

        SharedPrefHelper sp=new SharedPrefHelper(getContext());
        boolean status=sp.getTimeActivateStatus();
        Log.e("status", String.valueOf(status));
        if(!status){
            Log.e("onpausestatus","1");
            Intent intent =new Intent(getContext(), MainContainerActivity.class);
            startActivity(intent);
        }
    }


    @Override
    public  void onDestroy() {
        super.onDestroy();
        // Remove callbacks to prevent leaks
        timeCheckHandler.removeCallbacks(timeCheckRunnable);
    }
}

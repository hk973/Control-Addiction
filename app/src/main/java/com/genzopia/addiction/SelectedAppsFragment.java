package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
    private ImageView dragHandle;
    private LinearLayout expandedMenu;
    private boolean isMenuExpanded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main3, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupCircularMenu(view);

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
                requireActivity().finish(); // Close current activity


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


    @SuppressLint("ClickableViewAccessibility")
    private void setupCircularMenu(View rootView) {
        dragHandle = rootView.findViewById(R.id.drag_handle);
        expandedMenu = rootView.findViewById(R.id.expanded_menu);

        final int[] screenSize = new int[2];
        rootView.post(() -> {
            screenSize[0] = rootView.getWidth();
            screenSize[1] = rootView.getHeight();
        });

        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private long startTime;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startTime = System.currentTimeMillis();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Calculate absolute movement
                        float deltaX = Math.abs(event.getRawX() - (view.getX() + dX));
                        float deltaY = Math.abs(event.getRawY() - (view.getY() + dY));

                        if (deltaX > 5 || deltaY > 5) {
                            isDragging = true;
                            // Disable ViewPager scrolling
                            requestDisallowParentIntercept(true);
                        }

                        // Calculate new coordinates
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Constrain to screen boundaries
                        newX = Math.max(0, Math.min(newX, screenSize[0] - view.getWidth()));
                        newY = Math.max(0, Math.min(newY, screenSize[1] - view.getHeight()));

                        view.animate()
                                .x(newX)
                                .y(newY)
                                .setDuration(0)
                                .start();
                        return true;

                    case MotionEvent.ACTION_UP:
                        requestDisallowParentIntercept(false);

                        if (isDragging) {
                            snapToNearestCorner(view, screenSize[0], screenSize[1]);
                        } else {
                            // Handle click if under 200ms
                            if (System.currentTimeMillis() - startTime < 200) {
                                toggleMenu();
                            }
                        }
                        return true;

                    default:
                        return false;
                }
            }
        });
    }
    private void requestDisallowParentIntercept(boolean disallow) {
        ViewParent parent = dragHandle.getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
            parent = parent.getParent();
        }
    }


    private void snapToNearestCorner(View view, int screenWidth, int screenHeight) {
        final float[] corners = {
                0, 0, // Top-left
                screenWidth - view.getWidth(), 0, // Top-right
                0, screenHeight - view.getHeight(), // Bottom-left
                screenWidth - view.getWidth(), screenHeight - view.getHeight() // Bottom-right
        };

        float minDistance = Float.MAX_VALUE;
        int closestCorner = 0;

        for (int i = 0; i < corners.length; i += 2) {
            float distance = (float) Math.hypot(
                    view.getX() - corners[i],
                    view.getY() - corners[i + 1]
            );
            if (distance < minDistance) {
                minDistance = distance;
                closestCorner = i;
            }
        }

        view.animate()
                .x(corners[closestCorner])
                .y(corners[closestCorner + 1])
                .setDuration(200)
                .start();
    }

    private void toggleMenu() {
        if (isMenuExpanded) {
            collapseMenu();
        } else {
            expandMenu();
        }
    }

    private void expandMenu() {
        isMenuExpanded = true;
        expandedMenu.setVisibility(View.VISIBLE);

        // Position menu next to handle
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) expandedMenu.getLayoutParams();
        params.leftMargin = (int) dragHandle.getX() - expandedMenu.getWidth()/2;
        params.topMargin = (int) dragHandle.getY() - expandedMenu.getHeight()/2;
        expandedMenu.setLayoutParams(params);

        expandedMenu.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();
    }

    private void collapseMenu() {
        isMenuExpanded = false;
        expandedMenu.animate()
                .alpha(0f)
                .scaleX(0.5f)
                .scaleY(0.5f)
                .setDuration(300)
                .withEndAction(() -> expandedMenu.setVisibility(View.INVISIBLE))
                .start();
    }

}

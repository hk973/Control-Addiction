package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

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
    private FrameLayout dragHandle;
    private BillingClient billingClient;
    private SkuDetails targetSkuDetails; // To store fetched product details

    private boolean isMenuExpanded = false;
    private View overlay;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main3, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        initBillingClient();
        setupCircularMenu(view);

        searchBar = view.findViewById(R.id.searchBar);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));


        packageManager = requireActivity().getPackageManager();
        sharedPrefHelper = new SharedPrefHelper(requireContext());
        selectedApps = sharedPrefHelper.getSelectedAppValue();
        Log.e("test456", Arrays.toString(selectedApps.toArray()));

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


        // Position the handle at the right middle edge initially
        rootView.post(() -> {
            int screenWidth = rootView.getWidth();
            int screenHeight = rootView.getHeight();
            int handleWidth = dragHandle.getWidth();
            int handleHeight = dragHandle.getHeight();

            // Calculate right middle position
            float x = screenWidth - handleWidth;
            float y = (screenHeight - handleHeight) / 2f;

            dragHandle.setX(x);
            dragHandle.setY(y);
        });

        final int[] screenSize = new int[2];
        rootView.post(() -> {
            screenSize[0] = rootView.getWidth();
            screenSize[1] = rootView.getHeight();
        });

        // Add separate click listener for reliable click detection
        dragHandle.setOnClickListener(v -> {
            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_unlock);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            Button payToUnlockBtn = dialog.findViewById(R.id.payToUnlockBtn);
            payToUnlockBtn.setOnClickListener(view -> {
                // Trigger your payment flow here

                dialog.dismiss();
                if (targetSkuDetails != null) {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(targetSkuDetails)
                            .build();
                    billingClient.launchBillingFlow(getActivity(), flowParams);
                } else {
                    showMessage("Product not ready yet. Try again in a moment.");
                }
            });

            dialog.show();
        });


        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private float startX, startY;
            private boolean isDragging = false;
            private static final float CLICK_DISTANCE_THRESHOLD = 10f; // In pixels

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startX = event.getRawX();
                        startY = event.getRawY();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Calculate absolute movement
                        float deltaX = Math.abs(event.getRawX() - startX);
                        float deltaY = Math.abs(event.getRawY() - startY);

                        // Only consider as dragging if moved beyond threshold
                        if (!isDragging && (deltaX > CLICK_DISTANCE_THRESHOLD || deltaY > CLICK_DISTANCE_THRESHOLD)) {
                            isDragging = true;
                            // Disable ViewPager scrolling
                            requestDisallowParentIntercept(true);
                        }

                        // Only move if we're dragging
                        if (isDragging) {
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
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        requestDisallowParentIntercept(false);

                        if (isDragging) {
                            snapToNearestEdge(view, screenSize[0], screenSize[1]);
                            return true;
                        } else {
                            // Check if it's a small movement (potential click)
                            float totalMovement = Math.abs(event.getRawX() - startX) + Math.abs(event.getRawY() - startY);
                            if (totalMovement < CLICK_DISTANCE_THRESHOLD) {
                                // Let the click listener handle it
                                view.performClick();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        requestDisallowParentIntercept(false);
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



    private void snapToNearestEdge(View view, int screenWidth, int screenHeight) {
        // current position
        float curX = view.getX();
        float curY = view.getY();
        float vw = view.getWidth();
        float vh = view.getHeight();

        // Define the 4 candidate snap‐points by projecting onto each edge:
        //   Top edge:    x stays within [0, screenWidth-vw], y = 0
        //   Bottom edge: x stays within [0, screenWidth-vw], y = screenHeight-vh
        //   Left edge:   x = 0, y stays within [0, screenHeight-vh]
        //   Right edge:  x = screenWidth-vw, y stays within [0, screenHeight-vh]
        float[][] candidates = new float[4][2];

        // top
        candidates[0][0] = clamp(curX, 0, screenWidth - vw);
        candidates[0][1] = 0;

        // bottom
        candidates[1][0] = clamp(curX, 0, screenWidth - vw);
        candidates[1][1] = screenHeight - vh;

        // left
        candidates[2][0] = 0;
        candidates[2][1] = clamp(curY, 0, screenHeight - vh);

        // right
        candidates[3][0] = screenWidth - vw;
        candidates[3][1] = clamp(curY, 0, screenHeight - vh);

        // find the closest of the four
        float minDist = Float.MAX_VALUE;
        int   best   = 0;
        for (int i = 0; i < candidates.length; i++) {
            float dx = curX - candidates[i][0];
            float dy = curY - candidates[i][1];
            float dist = (float) Math.hypot(dx, dy);
            if (dist < minDist) {
                minDist = dist;
                best = i;
            }
        }

        // animate to it
        view.animate()
                .x(candidates[best][0])
                .y(candidates[best][1])
                .setDuration(200)
                .start();
    }

    /** simple clamp helper **/
    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
    private void initBillingClient() {
        billingClient = BillingClient.newBuilder(getContext())
                .enablePendingPurchases()
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            handlePurchase(purchase);
                        }
                    }
                })
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Retry connection if needed
            }
        });
    }


    private void queryProductDetails() {
        List<String> skuList = List.of("unlock_discipline_lock_v2");

        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
                .build();

        billingClient.querySkuDetailsAsync(params, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (SkuDetails skuDetails : skuDetailsList) {
                    if (skuDetails.getSku().equals("unlock_discipline_lock_v2")) {
                        targetSkuDetails = skuDetails;
                    }
                }
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        // Verify this is our product
        if (!purchase.getSkus().contains("unlock_discipline_lock_v2")) {
            return;
        }

        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        // Use weak reference to avoid context leaks
        WeakReference<SelectedAppsFragment> fragmentRef = new WeakReference<>(this);

        billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
            SelectedAppsFragment fragment = fragmentRef.get();
            Context context = fragment != null ? fragment.getContext() : null;

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Always save using application context
                Context appContext = context != null ? context.getApplicationContext() : requireContext().getApplicationContext();
                SharedPrefHelper prefHelper = new SharedPrefHelper(appContext);
                prefHelper.saveTimeLimitValue(0);
                prefHelper.saveTimeActivateStatus(false);

                // UI operations require attached fragment
                if (fragment != null && fragment.isAdded()) {
                    fragment.showMessage("Unlocked successfully!");
                } else {
                    // Fallback notification
                    Toast.makeText(appContext, "Unlocked successfully!", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (fragment != null && fragment.isAdded()) {
                    fragment.showMessage("Purchase failed. Try again.");
                } else if (context != null) {
                    Toast.makeText(context, "Purchase failed. Try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void showMessage(String msg) {
               new AlertDialog.Builder(getContext())
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();


    }



}

package com.genzopia.addiction.Launcher;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;

import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.QueryProductDetailsResult;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.genzopia.addiction.R;
import com.genzopia.addiction.data.AppRepository;
import com.genzopia.addiction.data.model.AppInfo;
import com.genzopia.addiction.ui.common.AppListAdapter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectedAppsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AppListAdapter adapter;
    private SharedPrefHelper sharedPrefHelper;
    private EditText searchBar;
    private AppRepository appRepository;

    // Billing
    private BillingClient billingClient;
    private ProductDetails targetProductDetails;

    // in‑memory lists
    private List<AppInfo> allApps = new ArrayList<>();
    private final List<String> selectedPackages = new ArrayList<>();

    // time‑check if you need to redirect on expiry
    private final Handler timeCheckHandler = new Handler(Looper.getMainLooper());
    private static final long CHECK_INTERVAL = 1_000;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main3, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPrefHelper = new SharedPrefHelper(requireContext());
        appRepository = AppRepository.getInstance(requireContext());

        // 1) init billing & menu
        initBillingClient();
        setupCircularMenu(view);

        // 2) set up RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchBar = view.findViewById(R.id.searchBar);
        // 3) load “selected” from prefs
        selectedPackages.clear();
        selectedPackages.addAll(sharedPrefHelper.getSelectedAppValue());

        // 4) create the shared adapter; launching follows the click-to-open preference
        adapter = new AppListAdapter(requireContext(), new AppListAdapter.Config()
                .launchOnClick(true)
                .clickToOpenAware(true));
        recyclerView.setAdapter(adapter);

        // 5) the repository keeps the installed-app list up to date for us
        appRepository.getApps().observe(getViewLifecycleOwner(), apps -> {
            allApps = apps == null ? new ArrayList<>() : apps;
            refreshList();
        });
        appRepository.refresh();

        // 6) hook up search bar
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 7) optionally start time checks
        timeCheckHandler.postDelayed(timeCheckRunnable, CHECK_INTERVAL);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timeCheckHandler.removeCallbacksAndMessages(null);
        if (billingClient != null) {
            try {
                billingClient.endConnection();
            } catch (Exception ignored) {
                // Already disconnected.
            }
            billingClient = null;
        }
    }

    /** Runnable that you already had for time‑expiry redirect */
    private final Runnable timeCheckRunnable = () -> {
        // The fragment may already be detached when this fires, so nothing here may use require*().
        Context ctx = getContext();
        if (ctx == null || !isAdded()) return;

        boolean active = new SharedPrefHelper(ctx).getTimeActivateStatus();
        if (!active) {
            Intent it = new Intent(ctx, MainContainerActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(it);
            if (getActivity() != null) {
                getActivity().finish();
            }
        } else {
            timeCheckHandler.postDelayed(this.timeCheckRunnable, CHECK_INTERVAL);
        }
    };

    /** Core toggle logic:
     *  if remainingTime <= 0 → show only “selected”
     *  else             → show all installed
     */
    private void refreshList() {
        if (adapter == null || !isAdded()) return;

        long remaining = sharedPrefHelper.getDSAChallengeRemainingTime() - System.currentTimeMillis();
        List<AppInfo> visible;
        ArrayList<String> allowed;

        if (remaining <= 0) {
            if (sharedPrefHelper.isDSAChallengeActive()) {
                // Challenge running with no earned time left: nothing is allowed.
                visible = Collections.emptyList();
                allowed = new ArrayList<>();
            } else {
                visible = appRepository.toAppInfos(selectedPackages);
                allowed = new ArrayList<>(selectedPackages);
            }
        } else {
            // Earned reward time: every installed app is temporarily allowed.
            visible = allApps;
            allowed = new ArrayList<>(appRepository.getAllPackages());
        }

        adapter.submitAppList(visible);
        sharedPrefHelper.set_selectedApps(allowed);
    }

    //─── billing helpers ─────────────────────────────────────────────────────────

    private void initBillingClient() {
        billingClient = BillingClient.newBuilder(getContext())
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && purchases != null) {
                        for (Purchase p : purchases) {
                            handlePurchase(p);
                        }
                    }
                })
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(@NonNull BillingResult br) {
                if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                }
            }
            @Override public void onBillingServiceDisconnected() {
                // retry logic if you want
            }
        });
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = List.of(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("unlock_discipline_lock_v2")
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        // ✅ Anonymous class avoids lambda type-inference ambiguity entirely
        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult,
                                                 @NonNull QueryProductDetailsResult result) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (ProductDetails details : result.getProductDetailsList()) {
                        if ("unlock_discipline_lock_v2".equals(details.getProductId())) {
                            targetProductDetails = details;
                            break;
                        }
                    }
                }
            }
        });
    }
    private void handlePurchase(Purchase purchase) {
        // ✅ Fix: use purchase.getProducts() instead of purchase.getSkus()
        if (!purchase.getProducts().contains("unlock_discipline_lock_v2")) return;

        ConsumeParams cp = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        WeakReference<SelectedAppsFragment> ref = new WeakReference<>(this);
        billingClient.consumeAsync(cp, (br, token) -> {
            SelectedAppsFragment frag = ref.get();
            Context ctx = frag != null ? frag.getContext() : null;
            if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Context appCtx = ctx != null
                        ? ctx.getApplicationContext()
                        : requireContext().getApplicationContext();

                SharedPrefHelper ph = new SharedPrefHelper(appCtx);
                ph.saveTimeLimitValue(0);
                ph.setDSAChallengeRemainingTime(0);
                ph.setDSAChallengeActive(false);
                ph.saveTimeActivateStatus(false);
                ph.setCheatChallengeValue(appCtx, true);

                if (frag != null && frag.isAdded()) {
                    frag.showMessage("Unlocked successfully!");
                }
            } else {
                if (frag != null && frag.isAdded()) {
                    frag.showMessage("Purchase failed. Try again.");
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

    //─── circular‑drag menu ──────────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private void setupCircularMenu(View rootView) {
        View dragHandle = rootView.findViewById(R.id.drag_handle);

        // initial position
        rootView.post(() -> {
            int w = rootView.getWidth(), h = rootView.getHeight();
            int hw = dragHandle.getWidth(), hh = dragHandle.getHeight();
            dragHandle.setX(w - hw);
            dragHandle.setY((h - hh) / 2f);
        });

        final int[] screenSize = new int[2];
        rootView.post(() -> {
            screenSize[0] = rootView.getWidth();
            screenSize[1] = rootView.getHeight();
        });

        // click → unlock dialog
        dragHandle.setOnClickListener(v -> {
            Dialog d = new Dialog(getContext());
            d.setContentView(R.layout.dialog_unlock);
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            Button btn = d.findViewById(R.id.payToUnlockBtn);
            btn.setOnClickListener(x -> {
                d.dismiss();
                if (targetProductDetails != null) {
                    // ✅ Fix: use ProductDetailsParams instead of setSkuDetails()
                    List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                            List.of(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(targetProductDetails)
                                            .build()
                            );

                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build();

                    billingClient.launchBillingFlow(getActivity(), flowParams);
                } else {
                    showMessage("Product not ready yet. Try again later.");
                }
            });
            d.show();
        });

        // drag logic
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY, startX, startY;
            private boolean dragging = false;
            private final float THRESH = 10f;

            @Override
            public boolean onTouch(View view, MotionEvent ev) {
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - ev.getRawX();
                        dY = view.getY() - ev.getRawY();
                        startX = ev.getRawX();
                        startY = ev.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(ev.getRawX() - startX);
                        float dy = Math.abs(ev.getRawY() - startY);
                        if (!dragging && (dx > THRESH || dy > THRESH)) {
                            dragging = true;
                            requestDisallowParentIntercept(view, true);
                        }
                        if (dragging) {
                            float nx = ev.getRawX() + dX;
                            float ny = ev.getRawY() + dY;
                            nx = clamp(nx, 0, screenSize[0] - view.getWidth());
                            ny = clamp(ny, 0, screenSize[1] - view.getHeight());
                            view.animate().x(nx).y(ny).setDuration(0).start();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        requestDisallowParentIntercept(view, false);
                        if (dragging) {
                            snapToNearestEdge(view, screenSize[0], screenSize[1]);
                            return true;
                        } else {
                            float total = Math.abs(ev.getRawX() - startX)
                                    + Math.abs(ev.getRawY() - startY);
                            if (total < THRESH) view.performClick();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        requestDisallowParentIntercept(view, false);
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void requestDisallowParentIntercept(View v, boolean disallow) {
        ViewParent p = v.getParent();
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(disallow);
            p = p.getParent();
        }
    }

    private void snapToNearestEdge(View v, int w, int h) {
        float x = v.getX(), y = v.getY();
        float vw = v.getWidth(), vh = v.getHeight();
        float[][] c = new float[4][2];

        // top
        c[0][0] = clamp(x, 0, w - vw); c[0][1] = 0;
        // bottom
        c[1][0] = clamp(x, 0, w - vw); c[1][1] = h - vh;
        // left
        c[2][0] = 0;                    c[2][1] = clamp(y, 0, h - vh);
        // right
        c[3][0] = w - vw;               c[3][1] = clamp(y, 0, h - vh);

        int best = 0;
        float minD = Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float dx = x - c[i][0], dy = y - c[i][1];
            float dist = (float)Math.hypot(dx, dy);
            if (dist < minD) { minD = dist; best = i; }
        }
        v.animate()
                .x(c[best][0])
                .y(c[best][1])
                .setDuration(200)
                .start();
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

}

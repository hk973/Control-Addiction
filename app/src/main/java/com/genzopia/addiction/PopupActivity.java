package com.genzopia.addiction;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.android.billingclient.api.*;

import java.util.List;

public class PopupActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private SkuDetails targetSkuDetails;
    private AlertDialog mainDialog; // Reference to main dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBillingClient();

        createAndShowMainDialog();
    }

    private void createAndShowMainDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("App Usage Alert")
                .setMessage("You cannot use this app as it is not in the approved list.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    startActivity(new Intent(PopupActivity.this, MainContainerActivity.class));
                    finish();
                })
                .setNegativeButton("Unlock All Apps", (dialog, which) -> {
                    if (targetSkuDetails != null) {
                        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(targetSkuDetails)
                                .build();
                        billingClient.launchBillingFlow(this, flowParams);
                    } else {
                        showMessage("Product not ready yet. Try again in a moment.");
                    }
                });

        // Create dialog but don't show immediately
        mainDialog = builder.create();

        // Only show if activity is active
        if (!isFinishing() && !isDestroyed()) {
            mainDialog.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dismiss dialog when activity is destroyed
        if (mainDialog != null && mainDialog.isShowing()) {
            mainDialog.dismiss();
        }
    }

    private void initBillingClient() {
        billingClient = BillingClient.newBuilder(this)
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
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                SharedPrefHelper prefHelper = new SharedPrefHelper(getApplicationContext());
                prefHelper.saveTimeLimitValue(0);
                prefHelper.saveTimeActivateStatus(false);
                showMessage("Unlocked successfully!");
            } else {
                showMessage("Purchase failed. Try again.");
            }
        });
    }

    private void showMessage(String msg) {
        runOnUiThread(() -> {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) return;

            try {
                new AlertDialog.Builder(PopupActivity.this)
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show();
            } catch (WindowManager.BadTokenException e) {
                // Log the error or handle it gracefully
            }
        });
    }
}
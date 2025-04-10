package com.genzopia.addiction;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.android.billingclient.api.*;

import java.util.List;

public class PopupActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private SkuDetails targetSkuDetails; // To store fetched product details

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBillingClient(); // Setup BillingClient

        // Create the popup dialog
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

        builder.create().show();
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
        // Consume the purchase to make it "consumable"
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.consumeAsync(consumeParams, (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Save time limit after successful purchase
                SharedPrefHelper prefHelper = new SharedPrefHelper(getApplicationContext());
                prefHelper.saveTimeLimitValue(0);
                prefHelper.saveTimeActivateStatus(false);

                // Show success message
                showMessage("Unlocked successfully!");
            } else {
                showMessage("Purchase failed. Try again.");
            }
        });
    }


    private void showMessage(String msg) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show());


    }
}

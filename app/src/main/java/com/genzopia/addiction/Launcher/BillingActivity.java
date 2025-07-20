package com.genzopia.addiction.Launcher;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.List;

public class BillingActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private SkuDetails targetSkuDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBillingClient();
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
                }).build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() { }
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

                        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                .setSkuDetails(skuDetails)
                                .build();

                        billingClient.launchBillingFlow(this, flowParams);
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
                prefHelper.setDSAChallengeRemainingTime(0);
                prefHelper.setDSAChallengeActive(false);
                runOnUiThread(() -> Toast.makeText(this, "Unlocked successfully!", Toast.LENGTH_LONG).show());
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Purchase failed. Try again.", Toast.LENGTH_SHORT).show());
            }

            finish(); // Close BillingActivity after purchase
        });
    }
}


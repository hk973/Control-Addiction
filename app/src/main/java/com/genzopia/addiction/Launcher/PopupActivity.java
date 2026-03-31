package com.genzopia.addiction.Launcher;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.android.billingclient.api.*;

import java.util.List;

public class PopupActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private SkuDetails targetSkuDetails;
    private ProductDetails targetProductDetails;
    private AlertDialog mainDialog; // Reference to main dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPrefHelper sp=new SharedPrefHelper(this) ;

        if(sp.isDSAChallengeActive()||sp.getTimeActivateStatus()){
        initBillingClient();
        createAndShowMainDialog();
        }


    }

    private void createAndShowMainDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("App Usage Alert")
                .setMessage("You cannot Uninstall this App when on Lock or Challenge Mode")
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
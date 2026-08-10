package com.genzopia.addiction.Launcher;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.android.billingclient.api.*;

import java.util.List;

public class PopupActivity extends AppCompatActivity {

    private static final String TAG = "PopupActivity";
    private static final String PRODUCT_ID = "unlock_discipline_lock_v2";
    private static final int MAX_RECONNECT_ATTEMPTS = 3;

    private BillingClient billingClient;
    private ProductDetails targetProductDetails;
    private AlertDialog mainDialog; // Reference to main dialog
    private int reconnectAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPrefHelper sp = new SharedPrefHelper(this);

        if (sp.isDSAChallengeActive() || sp.getTimeActivateStatus()) {
            initBillingClient();
            createAndShowMainDialog();
        } else {
            finish();
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
                .setNegativeButton("Unlock All Apps", (dialog, which) -> launchPurchase());

        // Create dialog but don't show immediately
        mainDialog = builder.create();

        // Only show if activity is active
        if (!isFinishing() && !isDestroyed()) {
            mainDialog.show();
        }
    }

    /** Starts the billing flow for the unlock product; no-op when the activity is going away. */
    private void launchPurchase() {
        if (isFinishing() || isDestroyed()) return;

        if (targetProductDetails == null || billingClient == null || !billingClient.isReady()) {
            showMessage("Product not ready yet. Try again in a moment.");
            return;
        }

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(List.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(targetProductDetails)
                                .build()))
                .build();

        BillingResult result = billingClient.launchBillingFlow(this, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            showMessage("Unable to start purchase. Try again later.");
        }
    }

    @Override
    protected void onDestroy() {
        // Dismiss dialog when activity is destroyed
        if (mainDialog != null && mainDialog.isShowing()) {
            mainDialog.dismiss();
        }
        mainDialog = null;
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
        super.onDestroy();
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

        connectBillingClient();
    }

    private void connectBillingClient() {
        if (billingClient == null) return;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(@NonNull BillingResult br) {
                if (isFinishing() || isDestroyed()) return;
                if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0;
                    queryProductDetails();
                }
            }
            @Override public void onBillingServiceDisconnected() {
                // Bounded retry — an unbounded reconnect loop would spin forever offline.
                if (isFinishing() || isDestroyed()) return;
                if (reconnectAttempts++ < MAX_RECONNECT_ATTEMPTS) {
                    connectBillingClient();
                } else {
                    Log.w(TAG, "Billing service unavailable after " + MAX_RECONNECT_ATTEMPTS + " retries");
                }
            }
        });
    }

    private void queryProductDetails() {
        if (billingClient == null) return;

        List<QueryProductDetailsParams.Product> productList = List.of(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        // Anonymous class avoids lambda type-inference ambiguity entirely
        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult,
                                                 @NonNull QueryProductDetailsResult result) {
                if (isFinishing() || isDestroyed()) return;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (ProductDetails details : result.getProductDetailsList()) {
                        if (PRODUCT_ID.equals(details.getProductId())) {
                            targetProductDetails = details;
                            break;
                        }
                    }
                }
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        if (billingClient == null) return;

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
                Log.w(TAG, "Unable to show message dialog", e);
            }
        });
    }
}

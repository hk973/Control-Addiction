package com.genzopia.addiction.Launcher;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;

import java.util.List;

public class BillingActivity extends AppCompatActivity {

    private static final String TAG = "BillingActivity";
    private static final String PRODUCT_ID = "unlock_discipline_lock_v2";
    private static final int MAX_RECONNECT_ATTEMPTS = 3;

    private BillingClient billingClient;
    private ProductDetails targetProductDetails;
    private int reconnectAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBillingClient();
    }

    @Override
    protected void onDestroy() {
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
                if (isActivityGone()) return;
                if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0;
                    queryProductDetails();
                }
            }
            @Override public void onBillingServiceDisconnected() {
                // Bounded retry — an unbounded reconnect loop would spin forever offline.
                if (isActivityGone()) return;
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
                if (isActivityGone()) return;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (ProductDetails details : result.getProductDetailsList()) {
                        if (PRODUCT_ID.equals(details.getProductId())) {
                            targetProductDetails = details;
                            break;
                        }
                    }
                    launchPurchase();
                }
            }
        });
    }

    /** Starts the billing flow; never runs against a finishing activity. */
    private void launchPurchase() {
        if (isActivityGone() || targetProductDetails == null
                || billingClient == null || !billingClient.isReady()) {
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
            Toast.makeText(this, "Unable to start purchase. Try again later.", Toast.LENGTH_SHORT).show();
        }
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
                showToast("Unlocked successfully!");
            } else {
                showToast("Purchase failed. Try again.");
            }

            if (!isActivityGone()) {
                finish(); // Close BillingActivity after purchase
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> {
            if (isActivityGone()) return;
            Toast.makeText(BillingActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private boolean isActivityGone() {
        return isFinishing() || isDestroyed();
    }
}

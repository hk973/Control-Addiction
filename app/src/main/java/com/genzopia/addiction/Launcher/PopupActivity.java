package com.genzopia.addiction.Launcher;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

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
    private AlertDialog mainDialog;
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
        if (isFinishing() || isDestroyed()) return;

        // Add strict lock friction before the main dialog
        int strictMode = StrictLockModeManager.getMode(this);
        if (strictMode != StrictLockModeManager.MODE_NORMAL) {
            showStrictLockChallenge(strictMode, this::showBlockingDialog);
        } else {
            showBlockingDialog();
        }
    }

    // ─── Strict lock challenges ───────────────────────────────────────────────

    private void showStrictLockChallenge(int mode, Runnable onSuccess) {
        if (isFinishing() || isDestroyed()) return;
        switch (mode) {
            case StrictLockModeManager.MODE_PIN:
                showPinChallenge(onSuccess);
                break;
            case StrictLockModeManager.MODE_MATH_CHALLENGE:
                showMathChallenge(onSuccess);
                break;
            case StrictLockModeManager.MODE_DELAY:
                showDelayChallenge(onSuccess);
                break;
            default:
                onSuccess.run();
        }
    }

    private void showPinChallenge(Runnable onSuccess) {
        if (isFinishing() || isDestroyed()) return;

        EditText pinInput = new EditText(this);
        pinInput.setHint("Enter 4-digit PIN");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

        new AlertDialog.Builder(this)
                .setTitle("🔐 Strict Lock")
                .setMessage("Enter your PIN to proceed:")
                .setView(pinInput)
                .setCancelable(false)
                .setPositiveButton("Verify", (d, w) -> {
                    String input = pinInput.getText().toString().trim();
                    if (StrictLockModeManager.validatePin(this, input)) {
                        onSuccess.run();
                    } else {
                        showMessage("Incorrect PIN");
                        goHome();
                    }
                })
                .setNegativeButton("Go Back", (d, w) -> goHome())
                .show();
    }

    private void showMathChallenge(Runnable onSuccess) {
        if (isFinishing() || isDestroyed()) return;

        StrictLockModeManager.MathChallenge challenge =
                StrictLockModeManager.generateMathChallenge();

        EditText answerInput = new EditText(this);
        answerInput.setHint("Your answer");
        answerInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

        new AlertDialog.Builder(this)
                .setTitle("🧮 Solve to Continue")
                .setMessage(challenge.question)
                .setView(answerInput)
                .setCancelable(false)
                .setPositiveButton("Check", (d, w) -> {
                    String input = answerInput.getText().toString().trim();
                    if (challenge.check(input)) {
                        onSuccess.run();
                    } else {
                        showMessage("Wrong answer! Try harder 💪");
                        goHome();
                    }
                })
                .setNegativeButton("Give Up", (d, w) -> goHome())
                .show();
    }

    private void showDelayChallenge(Runnable onSuccess) {
        if (isFinishing() || isDestroyed()) return;

        int delaySec = StrictLockModeManager.getDelaySec(this);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("⏳ Delay Challenge")
                .setCancelable(false)
                .create();

        TextView msgTv = new TextView(this);
        msgTv.setPadding(48, 32, 48, 32);
        msgTv.setTextSize(16f);
        msgTv.setText("Wait " + delaySec + " seconds before continuing…\n\n" + delaySec + "s remaining");
        dialog.setView(msgTv);

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Continue", (d, w) -> onSuccess.run());
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Go Back",  (d, w) -> goHome());
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        new CountDownTimer(delaySec * 1000L, 1000) {
            @Override public void onTick(long ms) {
                if (isFinishing() || isDestroyed() || !dialog.isShowing()) { cancel(); return; }
                msgTv.setText("Wait before continuing…\n\n" + (ms / 1000) + "s remaining");
            }
            @Override public void onFinish() {
                if (!isFinishing() && !isDestroyed() && dialog.isShowing()) {
                    msgTv.setText("✅ You may continue now.");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        }.start();
    }

    // ─── Main blocking dialog ─────────────────────────────────────────────────

    private void showBlockingDialog() {
        if (isFinishing() || isDestroyed()) return;

        mainDialog = new AlertDialog.Builder(this)
                .setTitle("App Usage Alert")
                .setMessage("You cannot Uninstall this App when on Lock or Challenge Mode")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    startActivity(new Intent(this, MainContainerActivity.class));
                    finish();
                })
                .setNegativeButton("Unlock All Apps", (d, w) -> launchPurchase())
                .create();

        if (!isFinishing() && !isDestroyed()) mainDialog.show();
    }

    private void goHome() {
        try { startActivity(new Intent(this, MainContainerActivity.class)); }
        catch (Exception ignored) {}
        finish();
    }

    // ─── Billing ─────────────────────────────────────────────────────────────

    private void launchPurchase() {
        if (isFinishing() || isDestroyed()) return;
        if (targetProductDetails == null || billingClient == null || !billingClient.isReady()) {
            showMessage("Product not ready yet. Try again in a moment.");
            return;
        }
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(List.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(targetProductDetails).build()))
                .build();
        BillingResult result = billingClient.launchBillingFlow(this, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            showMessage("Unable to start purchase. Try again later.");
        }
    }

    @Override
    protected void onDestroy() {
        if (mainDialog != null && mainDialog.isShowing()) mainDialog.dismiss();
        mainDialog = null;
        if (billingClient != null) { billingClient.endConnection(); billingClient = null; }
        super.onDestroy();
    }

    private void initBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener((br, purchases) -> {
                    if (br.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null)
                        for (Purchase p : purchases) handlePurchase(p);
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
                    reconnectAttempts = 0; queryProductDetails();
                }
            }
            @Override public void onBillingServiceDisconnected() {
                if (isFinishing() || isDestroyed()) return;
                if (reconnectAttempts++ < MAX_RECONNECT_ATTEMPTS) connectBillingClient();
                else Log.w(TAG, "Billing unavailable after " + MAX_RECONNECT_ATTEMPTS + " retries");
            }
        });
    }

    private void queryProductDetails() {
        if (billingClient == null) return;
        billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder()
                        .setProductList(List.of(QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()))
                        .build(),
                new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult br,
                                                         @NonNull QueryProductDetailsResult result) {
                        if (isFinishing() || isDestroyed()) return;
                        if (br.getResponseCode() == BillingClient.BillingResponseCode.OK)
                            for (ProductDetails d : result.getProductDetailsList())
                                if (PRODUCT_ID.equals(d.getProductId())) { targetProductDetails = d; break; }
                    }
                });
    }

    private void handlePurchase(Purchase purchase) {
        if (billingClient == null) return;
        billingClient.consumeAsync(
                ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                (br, token) -> {
                    if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        SharedPrefHelper p = new SharedPrefHelper(getApplicationContext());
                        p.saveTimeLimitValue(0);
                        p.saveTimeActivateStatus(false);
                        p.setDSAChallengeRemainingTime(0);
                        p.setDSAChallengeActive(false);
                        showMessage("Unlocked successfully!");
                    } else {
                        showMessage("Purchase failed. Try again.");
                    }
                });
    }

    private void showMessage(String msg) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                new AlertDialog.Builder(this).setMessage(msg).setPositiveButton("OK", null).show();
            } catch (WindowManager.BadTokenException e) {
                Log.w(TAG, "Unable to show dialog", e);
            }
        });
    }
}

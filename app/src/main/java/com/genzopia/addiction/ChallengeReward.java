package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

public class ChallengeReward extends AppCompatActivity {

    private static final String APP_SECRET_KEY = "YourAppSecretKey123!";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_reward);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        SharedPrefHelper sp = new SharedPrefHelper(this);
        sp.setChallengeStatus(this, false);
        String uniqueCode="";
        if(sp.getCheatChallengeValue(this)){
            Intent intent=new Intent(this,MainContainerActivity.class);
            startActivity(intent);
            finish();
        }{
        // Retrieve reward code
        if (getIntent().hasExtra("REWARD_CODE")) {
             uniqueCode = getIntent().getStringExtra("REWARD_CODE");
            // Use the code as needed
        }else {


             uniqueCode= generateUniqueCode();
            sp.set_challenge_code_List(this, uniqueCode);
        }
        // Set coupon code in UI
        TextView couponCodeView = findViewById(R.id.coupon_code);
        couponCodeView.setText(uniqueCode);

        // Setup button action
        findViewById(R.id.visit_button).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.genzopia.com"));
            startActivity(browserIntent);
        });

        // Add star animation
        animateStar();}
    }

    private void animateStar() {
        ImageView star = findViewById(R.id.star_icon);
        star.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(500)
                .withEndAction(() -> star.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(500))
                .start();
    }


    public String generateUniqueCode() {
        try {
            // 1. Generate positive random part (10 digits)
            SecureRandom random = new SecureRandom();
            long randomPart = Math.abs(random.nextLong()) % 10000000000L;

            // 2. Get timestamp part (3 digits - last 3 digits of current time)
            long timePart = new Date().getTime() % 1000;

            // 3. Combine to make base code (13 digits)
            String baseCode = String.format("%010d%03d", randomPart, timePart);

            // 4. Generate verification signature (2 digits)
            String signature = generateSignature(baseCode).substring(0, 2);

            // 5. Final 15-digit code
            String finalCode = baseCode + signature;

            // Ensure we have exactly 15 digits
            if (finalCode.length() != 15) {
                throw new RuntimeException("Generated code is not 15 digits: " + finalCode);
            }

            return finalCode;

        } catch (Exception e) {
            Log.e("CodeGeneration", "Error generating code", e);
            // Fallback - guaranteed 15 digits
            return String.format("%015d", Math.abs(new SecureRandom().nextLong())).substring(0, 15);
        }
    }

    private String generateSignature(String input) throws NoSuchAlgorithmException {
        String data = input + APP_SECRET_KEY;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        String base64 = Base64.encodeToString(hash, Base64.NO_WRAP | Base64.URL_SAFE);
        return base64.replaceAll("[^0-9]", "");
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent=new Intent(this,MainContainerActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPrefHelper sp=new SharedPrefHelper(this);
        sp.setCheatChallengeValue(this,false);
    }
}
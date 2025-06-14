package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

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
        SharedPrefHelper sp = new SharedPrefHelper(this);
        sp.setChallengeStatus(this, false);
        String uniqueCode = generateUniqueCode();
        sp.set_challenge_code_List(this,uniqueCode);
        Log.d("CodeGeneration", "Generated Code: " + sp.get_challenge_code_list(this));
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
}
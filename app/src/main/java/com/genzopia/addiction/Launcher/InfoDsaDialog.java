package com.genzopia.addiction.Launcher;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.genzopia.addiction.R;
import com.google.android.material.button.MaterialButton;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InfoDsaDialog extends Dialog {
    private final MainFragment hostFragment;
    private boolean isProfileVerified = false;
    int leetcodescore=0;

    public InfoDsaDialog(@NonNull MainFragment fragment) {
        super(fragment.requireContext());
        this.hostFragment = fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_dsa_config);

        EditText etHours = findViewById(R.id.etMinutesPerProblem);
        MaterialButton btnVerify = findViewById(R.id.btnVerify);
        EditText etProfileUrl = findViewById(R.id.etProfileUrl);
        ImageView infoIcon = findViewById(R.id.infoIcon);
        MaterialButton btnStart = findViewById(R.id.btnStartChall);

        if (etHours == null || btnVerify == null || etProfileUrl == null
                || infoIcon == null || btnStart == null) {
            Log.e("InfoDsaDialog", "One or more views could not be found.");
            dismiss();
            return;
        }

        etProfileUrl.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

        btnVerify.setOnClickListener(v -> {
            String profileName = etProfileUrl.getText().toString().trim();
            if (TextUtils.isEmpty(profileName)) {
                etProfileUrl.setError("Profile name cannot be empty");
                return;
            }

            btnVerify.setEnabled(false);
            etProfileUrl.setEnabled(false);

            fetchLeetcodeScore(profileName, score -> {
                if (!isShowing() || !hostFragment.isAdded()) return;

                if (score != -1) {
                    isProfileVerified = true;
                    etProfileUrl.setTextColor(
                            ContextCompat.getColor(getContext(), R.color.disabled_text)
                    );
                    etProfileUrl.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_check_circle, 0
                    );
                    Toast.makeText(getContext(),
                            "LeetCode solved count: " + score,
                            Toast.LENGTH_SHORT).show();
                     leetcodescore = score;
                } else {
                    Toast.makeText(getContext(),
                            "Wrong profile name entered",
                            Toast.LENGTH_SHORT).show();
                    etProfileUrl.setEnabled(true);
                }
                btnVerify.setEnabled(true);
            });
        });

        infoIcon.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Info")
                    .setMessage("This is the number of hours you want your phone "
                            + "to remain unlocked when one LeetCode problem is solved.")
                    .setPositiveButton("OK", null)
                    .show();
        });

        btnStart.setOnClickListener(v -> {
            if (!hostFragment.isAccessibilityServiceEnabled(
                    getContext(), NotificationBarDetectorService.class)) {
                hostFragment.showAccessibilityDialog();
                return;
            }

            if (!isProfileVerified) {
                Toast.makeText(getContext(),
                        "Please verify your LeetCode profile first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String hoursStr = etHours.getText().toString().trim();
            if (TextUtils.isEmpty(hoursStr)) {
                etHours.setError("Enter hours per problem");
                return;
            }

            int hours;
            try {
                hours = Integer.parseInt(hoursStr);
                if (hours <= 0 || hours > 24) {
                    etHours.setError("Please enter a value from 1 to 24");
                    return;
                }
            } catch (NumberFormatException nfe) {
                etHours.setError("Invalid number");
                return;
            }

            showStartConfirmation(etProfileUrl.getText().toString(), hours);
        });

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showStartConfirmation(String username, int hoursPerProblem) {
        new AlertDialog.Builder(getContext())
                .setTitle("DSA Challenge Warning")
                .setMessage("After starting, you will only be able to use your phone "
                        + "after solving a LeetCode problem. Once your usage time expires, "
                        + "you must solve another problem or pay to unlock.")
                .setPositiveButton("Start Challenge", (dialog, which) -> {
                    startChallenge(username, hoursPerProblem);
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void startChallenge(String username, int hoursPerProblem) {
        executeMainLogic(hoursPerProblem,username);
    }
    private ArrayList<String> getAllLaunchableAppPackageNames() {
        ArrayList<String> packageNameList = new ArrayList<>();

        PackageManager pm = getContext().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {
            // Only include apps that can be launched (i.e., have a launcher intent)
            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                packageNameList.add(app.packageName);
            }
        }

        return packageNameList;
    }


    private void fetchLeetcodeScore(String username, LeetcodeScoreCallback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        String payload = "{"
                + "\"query\":\"query getUserProfile($username: String!) { "
                + "matchedUser(username: $username) { submitStatsGlobal { acSubmissionNum { count } } } }\","
                + "\"variables\":{\"username\":\"" + username + "\"}"
                + "}";

        RequestBody body = RequestBody.create(
                payload, MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url("https://leetcode.com/graphql")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("InfoDsaDialog", "Network error", e);
                mainHandler.post(() -> callback.onScoreFetched(-1));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                int totalSolved = -1; // -1 = user not found or error

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonStr = response.body().string();
                        JSONObject root = new JSONObject(jsonStr);
                        JSONObject data = root.optJSONObject("data");
                        if (data != null) {
                            JSONObject matchedUser = data.optJSONObject("matchedUser");
                            if (matchedUser != null) {
                                JSONArray arr = matchedUser
                                        .optJSONObject("submitStatsGlobal")
                                        .optJSONArray("acSubmissionNum");

                                totalSolved = 0; // reset to zero when user exists
                                if (arr != null) {
                                    for (int i = 0; i < arr.length(); i++) {
                                        totalSolved += arr.getJSONObject(i).optInt("count", 0);
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Log.e("InfoDsaDialog", "Parsing error", ex);
                    }
                }

                final int score = totalSolved;
                mainHandler.post(() -> callback.onScoreFetched(score));
            }
        });
    }

    /** Callback for delivering LeetCode solved-count (or -1 on failure). */
    public interface LeetcodeScoreCallback {
        void onScoreFetched(int score);
    }
    private void executeMainLogic(long challengeHours, String username) {
     SharedPrefHelper sp=new SharedPrefHelper(getContext());
     sp.setDSAChallengeActive(true);
     sp.set_current_leetcode(leetcodescore);
     sp.setDSAChallengeRemainingTime(System.currentTimeMillis());
     sp.setPerQuestionTime(challengeHours*60L*60L*1000L);
     sp.setLeetCodeUsername(username);
     sp.settimmerzero(true);
     hostFragment.launchDeviceCredentialVerification(30);
    }


}

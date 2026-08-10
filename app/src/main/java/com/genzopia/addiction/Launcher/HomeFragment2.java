package com.genzopia.addiction.Launcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.genzopia.addiction.R;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment2 extends Fragment {

    /** One shared client: building an OkHttpClient per request wastes threads and sockets. */
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Handler handler;
    private Runnable updateRunnable;
    private TextView timerText;
    private TextView leetcodeScoreText;
    private LinearLayout dsaInactiveLayout;
    private LinearLayout bottomBar;
    private ImageButton syncButton;
    private SharedPrefHelper spp;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activty_home_selected, container, false);
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupStatusBar();
        setupShortcuts();

        // Initialize views
        timerText = view.findViewById(R.id.textView_time);
        leetcodeScoreText = view.findViewById(R.id.leetcodeScoreText);
        dsaInactiveLayout = view.findViewById(R.id.dsaInactiveLayout);
        bottomBar = view.findViewById(R.id.bottomBar);
        syncButton = view.findViewById(R.id.syncButton);
        spp = new SharedPrefHelper(getContext());

        // Setup sync button
        syncButton.setOnClickListener(v -> {

            long initialScore = spp.get_current_leetcode();

            if (spp.isDSAChallengeActive()) {
                fetchLeetcodeScore(new LeetcodeScoreCallback() {
                    @Override
                    public void onSuccess(int fetchedScore) {
                        if (!isAdded() || getView() == null) return;
                        if (fetchedScore > initialScore) {
                            // ✅ Do something on improvement
                            Toast.makeText(getContext(), "Score improved! 🎉", Toast.LENGTH_SHORT).show();
                            long diff=fetchedScore-initialScore;
                            long milisecs=diff*spp.getPerQuestionTime();
                            long remaingsec=spp.getDSAChallengeRemainingTime();
                            spp.setDSAChallengeRemainingTime(remaingsec+milisecs);
                            spp.set_current_leetcode(fetchedScore);


                        } else {
                            // ❌ No improvement
                            Toast.makeText(getContext(), "No improvement in score.", Toast.LENGTH_SHORT).show();
                        }
                        leetcodeScoreText.setText("LeetCode Score: " + fetchedScore);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (!isAdded() || getView() == null) return;
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        leetcodeScoreText.setText("Error");
                    }
                });
            }
        });

        // Check DSA challenge status
        checkDsaChallengeStatus();
        showShortcutIcon(view);
    }

    /** Paints the shortcut button with the icon of the assigned app, if it is still installed. */
    private void showShortcutIcon(View view) {
        Context context = getContext();
        if (context == null) return;

        ImageView shortcutButton = view.findViewById(R.id.cameraButton);
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0); // 0 = grayscale
        shortcutButton.setColorFilter(new ColorMatrixColorFilter(matrix));

        String shortcutPackage = new SharedPrefHelper(context).getString(context, "shortcut", "");
        if (shortcutPackage == null || shortcutPackage.isEmpty()) return;

        try {
            PackageManager pm = context.getPackageManager();
            shortcutButton.setImageDrawable(pm.getApplicationInfo(shortcutPackage, 0).loadIcon(pm));
        } catch (PackageManager.NameNotFoundException e) {
            // Shortcut target uninstalled — keep the default icon.
            Log.d("HomeFragment2", "Shortcut app not installed: " + shortcutPackage);
        }
    }

    private void checkDsaChallengeStatus() {
        SharedPrefHelper sp = new SharedPrefHelper(requireContext());
        boolean isChallengeActive = sp.isDSAChallengeActive();

        if (!isChallengeActive) {
            // Show challenge inactive UI

            timerText.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            dsaInactiveLayout.setVisibility(View.GONE);
            setupChallengeActiveState();
        } else {
            // Show challenge active UI
            timerText.setVisibility(View.VISIBLE);
            dsaInactiveLayout.setVisibility(View.VISIBLE);
            leetcodeScoreText.setText("Loading");
            fetchLeetcodeScore(new LeetcodeScoreCallback() {
                @Override
                public void onSuccess(int fetchedScore) {
                    if (!isAdded() || getView() == null) return;
                    leetcodeScoreText.setText("LeetCode Score: " + fetchedScore);
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (!isAdded() || getView() == null) return;
                    leetcodeScoreText.setText("Fetching failed ...");
                }
            });
        }
    }

    private void setupChallengeActiveState() {
        if (getView() != null) {
            showShortcutIcon(getView());
        }
    }

    private void fetchLeetcodeScore(LeetcodeScoreCallback callback) {
        SharedPrefHelper sp = new SharedPrefHelper(requireContext());
        String username = sp.getLeetCodeUsername();

        if (username == null || username.isEmpty()) {
            callback.onFailure("Username not set");
            return;
        }

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

        HTTP_CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onFailure("Unable to fetch data. Please check your internet connection."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    response.close();
                    mainHandler.post(() -> callback.onFailure("Server error"));
                    return;
                }

                try {
                    String jsonStr = response.body().string();
                    JSONObject root = new JSONObject(jsonStr);
                    JSONObject data = root.optJSONObject("data");
                    int totalSolved = -1;

                    if (data != null) {
                        JSONObject matchedUser = data.optJSONObject("matchedUser");
                        if (matchedUser != null) {
                            JSONArray arr = matchedUser
                                    .getJSONObject("submitStatsGlobal")
                                    .getJSONArray("acSubmissionNum");

                            totalSolved = 0;
                            for (int i = 0; i < arr.length(); i++) {
                                totalSolved += arr.getJSONObject(i).getInt("count");
                            }
                        }
                    }

                    // -1 means "no such user"; dividing first would turn it into a valid 0.
                    final int finalScore = totalSolved < 0 ? -1 : totalSolved / 2;
                    mainHandler.post(() -> {
                        if (finalScore >= 0) {
                            callback.onSuccess(finalScore);
                        } else {
                            callback.onFailure("User not found");
                        }
                    });

                } catch (Exception e) {
                    Log.e("LeetCode", "Parsing error", e);
                    mainHandler.post(() -> callback.onFailure("Error parsing data"));
                } finally {
                    response.close();
                }
            }
        });
    }



    // Rest of the class remains the same (onResume, onPause, timer methods, etc.)
    @Override
    public void onResume() {
        super.onResume();
        if( spp.isDSAChallengeActive()){
            startRealtimeUpdatesdsa();
        }else{
        startRealtimeUpdates();}
    }

    private void startRealtimeUpdatesdsa() {
        stopRealtimeUpdates();
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdowndsa();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
    }



    @Override
    public void onPause() {
        super.onPause();
        stopRealtimeUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRealtimeUpdates();
        mainHandler.removeCallbacksAndMessages(null);
        timerText = null;
        leetcodeScoreText = null;
        dsaInactiveLayout = null;
        bottomBar = null;
        syncButton = null;
    }

    private void startRealtimeUpdates() {
        stopRealtimeUpdates();
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdown();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
    }

    private void stopRealtimeUpdates() {
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private void updateCountdown() {
        // The ticker can outlive the view by one frame, so bail out instead of crashing.
        if (getContext() == null || timerText == null) return;
        long remaintime = new SharedPrefHelper(getContext()).getRemainingTimeMillis();
        timerText.setText(formatTime(remaintime / 1000));
    }
    private void updateCountdowndsa() {
        if (timerText == null || spp == null) return;
        long now = System.currentTimeMillis();
        long endTime = spp.getDSAChallengeRemainingTime();
        long remainingMillis = endTime - now;

        if (remainingMillis > 0) {
            timerText.setText(formatTimeDSA(remainingMillis));
            spp.settimmerzero(false);
        } else {
            timerText.setText("00:00");
            spp.setDSAChallengeRemainingTime(System.currentTimeMillis());
            spp.settimmerzero(true);
        }
    }


    private String formatTimeDSA(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }


    public String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("Time Remaining: %02d:%02d:%02d", hours, minutes, seconds);
    }

    private void setupStatusBar() {
        Window window = requireActivity().getWindow();
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isSystemDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        int statusBarColor = isSystemDarkMode ?
                ContextCompat.getColor(requireContext(), R.color.black) :
                ContextCompat.getColor(requireContext(), R.color.white);

        window.setStatusBarColor(statusBarColor);

        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(
                window, window.getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isSystemDarkMode);
    }

    private void setupShortcuts() {
        ImageView phoneButton = requireView().findViewById(R.id.phoneButton);
        ImageView cameraButton = requireView().findViewById(R.id.cameraButton);

        phoneButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL));
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No dialer app found", Toast.LENGTH_SHORT).show();
            }
        });
        cameraButton.setOnClickListener(v -> {
            if (isAdded()) new PopupSelectApp(requireContext()).showlock(cameraButton);
        });


    }
    public interface LeetcodeScoreCallback {
        void onSuccess(int fetchedScore);
        void onFailure(String errorMessage); // To handle no internet/server error
    }

}
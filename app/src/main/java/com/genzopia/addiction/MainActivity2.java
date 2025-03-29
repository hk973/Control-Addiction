package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private MaterialButton buttonSetTime;
    private AppAdapter appAdapter;
    private ArrayList<String> selectedApps = new ArrayList<>();
    private boolean isTimeSet = false;
    private int selectedDays = 0;
    private int selectedHours = 0;
    private int selectedMinutes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Window window = getWindow();
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isSystemDarkMode = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

        int statusBarColor = isSystemDarkMode ? getResources().getColor(R.color.black, getTheme())
                : getResources().getColor(R.color.white, getTheme());

        window.setStatusBarColor(statusBarColor);

        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        windowInsetsController.setAppearanceLightStatusBars(!isSystemDarkMode);

        // Initialize views first
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        TextView titleTextView = findViewById(R.id.titleTextView);
        searchBar = findViewById(R.id.searchBar);
        recyclerView = findViewById(R.id.recyclerView); // Use class field, don't create local variable
        buttonSetTime = findViewById(R.id.buttonSetTime);
        LinearLayout buttonContainer = (LinearLayout) buttonSetTime.getParent();

        // Then apply theme and load apps
        applyTheme(appBarLayout, titleTextView, recyclerView, buttonContainer);
        loadApps();
        checksp();

        buttonSetTime.setOnClickListener(view -> {
            if (!isTimeSet) {
                showTimePickerDialog();
            } else {
                proceedWithSelectedTime();
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                appAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateTimeDisplay() {
        String timeText = "";
        if (selectedDays > 0) timeText += selectedDays + "d ";
        if (selectedHours > 0) timeText += selectedHours + "h ";
        if (selectedMinutes > 0) timeText += selectedMinutes + "m";

        if (!timeText.isEmpty()) {
            buttonSetTime.setText("Lock Apps (" + timeText.trim() + ")");
        }
    }

    private void showTimePickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        NumberPicker daysPicker = dialogView.findViewById(R.id.daysPicker);
        NumberPicker hoursPicker = dialogView.findViewById(R.id.hoursPicker);
        NumberPicker minutesPicker = dialogView.findViewById(R.id.minutesPicker);

        // Setup NumberPickers
        daysPicker.setMinValue(0);
        daysPicker.setMaxValue(30);
        daysPicker.setValue(selectedDays);

        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(23);
        hoursPicker.setValue(selectedHours);

        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(59);
        minutesPicker.setValue(selectedMinutes);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog_Alert);
        AlertDialog dialog = builder.setView(dialogView)
                .setPositiveButton("Set", (d, which) -> {
                    selectedDays = daysPicker.getValue();
                    selectedHours = hoursPicker.getValue();
                    selectedMinutes = minutesPicker.getValue();

                    if (selectedDays == 0 && selectedHours == 0 && selectedMinutes == 0) {
                        Toast.makeText(this, "Please set a time limit", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    isTimeSet = true;
                    updateTimeDisplay();
                })
                .setNegativeButton("Cancel", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(android.view.Gravity.CENTER);
        }

        dialog.show();
    }

    private void applyTheme(AppBarLayout appBarLayout, TextView titleTextView,
                            RecyclerView recyclerView, View buttonContainer) {
        // Check if the device is in dark mode
        boolean isDarkMode = (getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            // Apply dark theme
            int darkColor = ContextCompat.getColor(this, android.R.color.black);
            int textColor = ContextCompat.getColor(this, android.R.color.white);

            appBarLayout.setBackgroundColor(darkColor);
            recyclerView.setBackgroundColor(darkColor);
            buttonContainer.setBackgroundColor(darkColor);
            titleTextView.setTextColor(textColor);

            // You might want to update other UI elements like search hint color, etc.

        } else {
            // Apply light theme
            int lightColor = ContextCompat.getColor(this, android.R.color.white);
            int textColor = ContextCompat.getColor(this, android.R.color.black);

            appBarLayout.setBackgroundColor(lightColor);
            recyclerView.setBackgroundColor(lightColor);
            buttonContainer.setBackgroundColor(lightColor);
            titleTextView.setTextColor(textColor);
        }
    }

    private void checksp() {
        SharedPrefHelper sp=new SharedPrefHelper(this);
         boolean status= sp.getTimeActivateStatus();
        if(status){
            Intent intent =new Intent(this,MainActivity3.class);
            startActivity(intent);
        }
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.MATCH_ALL);
        List<AppItem_Dataclass> appItems = new ArrayList<>();

        for (ApplicationInfo appInfo : apps) {
            Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
            if (launchIntent != null) {
                String appName = pm.getApplicationLabel(appInfo).toString();
                appItems.add(new AppItem_Dataclass(appName, appInfo.packageName));
            }
        }

        appAdapter = new AppAdapter(appItems, selectedApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(appAdapter);
    }

    private void proceedWithSelectedTime() {
        if (selectedApps.isEmpty()) {
            Toast.makeText(this, "Please select at least one app", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalMinutes = (selectedDays * 24 * 60) + (selectedHours * 60) + selectedMinutes;
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(this);
        sharedPrefHelper.writeData(selectedApps, totalMinutes, true);

        Intent intent = new Intent(MainActivity2.this, MainActivity3.class);
        startActivity(intent);
        finish();
    }




    private boolean isValidInput(String timeInput) {
        return !TextUtils.isEmpty(timeInput) && !timeInput.equals("0") && !selectedApps.isEmpty();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {

    }
}

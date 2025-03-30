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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainFragment extends Fragment {

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the original MainActivity2 layout
        return inflater.inflate(R.layout.activity_main2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupStatusBar();
        initializeViews();
        checksp();
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

    private void initializeViews() {
        // Initialize views using fragment's view
        ImageView settingsButton = requireView().findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), SettingsActivity.class)));

        AppBarLayout appBarLayout = requireView().findViewById(R.id.appBarLayout);
        TextView titleTextView = requireView().findViewById(R.id.titleTextView);
        searchBar = requireView().findViewById(R.id.searchBar);
        recyclerView = requireView().findViewById(R.id.recyclerView);
        buttonSetTime = requireView().findViewById(R.id.buttonSetTime);
        LinearLayout buttonContainer = (LinearLayout) buttonSetTime.getParent();

        applyTheme(appBarLayout, titleTextView, recyclerView, buttonContainer);
        loadApps();

        buttonSetTime.setOnClickListener(view -> {
            if (!isTimeSet) {
                showTimePickerDialog();
            } else {
                proceedWithSelectedTime();
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                appAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // All original MainActivity2 methods below - unchanged except context access

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
        Button buttonSet = dialogView.findViewById(R.id.buttonSet);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        daysPicker.setMinValue(0);
        daysPicker.setMaxValue(30);
        daysPicker.setValue(selectedDays);

        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(23);
        hoursPicker.setValue(selectedHours);

        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(59);
        minutesPicker.setValue(selectedMinutes);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        AlertDialog dialog = builder.setView(dialogView).create();

        buttonSet.setOnClickListener(v -> {
            selectedDays = daysPicker.getValue();
            selectedHours = hoursPicker.getValue();
            selectedMinutes = minutesPicker.getValue();

            if (selectedDays == 0 && selectedHours == 0 && selectedMinutes == 0) {
                Toast.makeText(requireContext(), "Please set a time limit", Toast.LENGTH_SHORT).show();
                return;
            }

            isTimeSet = true;
            updateTimeDisplay();
            dialog.dismiss();
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(android.view.Gravity.CENTER);
        }

        dialog.show();
    }

    private void applyTheme(AppBarLayout appBarLayout, TextView titleTextView,
                            RecyclerView recyclerView, View buttonContainer) {
        boolean isDarkMode = (getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            int darkColor = ContextCompat.getColor(requireContext(), android.R.color.black);
            int textColor = ContextCompat.getColor(requireContext(), android.R.color.white);

            appBarLayout.setBackgroundColor(darkColor);
            recyclerView.setBackgroundColor(darkColor);
            buttonContainer.setBackgroundColor(darkColor);
            titleTextView.setTextColor(textColor);

        } else {
            int lightColor = ContextCompat.getColor(requireContext(), android.R.color.white);
            int textColor = ContextCompat.getColor(requireContext(), android.R.color.black);

            appBarLayout.setBackgroundColor(lightColor);
            recyclerView.setBackgroundColor(lightColor);
            buttonContainer.setBackgroundColor(lightColor);
            titleTextView.setTextColor(textColor);
        }
    }

    private void checksp() {
        SharedPrefHelper sp = new SharedPrefHelper(requireContext());
        boolean status = sp.getTimeActivateStatus();
        if(status){
            startActivity(new Intent(requireActivity(), MainActivity3.class));
        }
    }

    private void loadApps() {
        PackageManager pm = requireActivity().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.MATCH_ALL);
        List<AppItem_Dataclass> appItems = new ArrayList<>();

        for (ApplicationInfo appInfo : apps) {
            Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
            if (launchIntent != null) {
                String appName = pm.getApplicationLabel(appInfo).toString();
                appItems.add(new AppItem_Dataclass(appName, appInfo.packageName));
            }
        }

        appAdapter = new AppAdapter(appItems, selectedApps, requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(appAdapter);
    }

    private void proceedWithSelectedTime() {
        if (selectedApps.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one app", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalMinutes = (selectedDays * 24 * 60) + (selectedHours * 60) + selectedMinutes;
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(requireContext());
        sharedPrefHelper.writeData(selectedApps, totalMinutes, true);

        startActivity(new Intent(requireActivity(), MainActivity3.class));
        requireActivity().finish();
    }

    private boolean isValidInput(String timeInput) {
        return !TextUtils.isEmpty(timeInput) && !timeInput.equals("0") && !selectedApps.isEmpty();
    }
}
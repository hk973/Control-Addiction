package com.genzopia.addiction;



import static android.app.Activity.RESULT_OK;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MainFragment extends Fragment {


    private RecyclerView recyclerView;
    private EditText searchBar;
    private MaterialButton buttonSetTime;
    private AppAdapter appAdapter;
    private ArrayList<String> selectedApps = new ArrayList<>();
    private boolean isTimeSet = false;
    int selectedDays = 0;
     int selectedHours = 0;
     int selectedMinutes = 0;
    private AppListViewModel viewModel;
    private ProgressBar progressBar;
    private SeekBar.OnSeekBarChangeListener seekBarChangeListener;
    private static final int REQUEST_CODE_ACCESSIBILITY_PERMISSION = 102;
    AuthenticationManager aa;



    @Override
    public void onResume() {
        super.onResume();
        checksp();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main2, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AppListViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupStatusBar();
        initializeViews();
        checksp();

        // Remove progress bar since we expect instant load
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
          aa = AuthenticationManager.getInstance();

        AppListViewModel viewModel = new ViewModelProvider(requireActivity()).get(AppListViewModel.class);
        viewModel.getAppItemsLiveData().observe(getViewLifecycleOwner(), appItems -> {
            if (appItems != null) {
                setupRecyclerView(appItems);
                refreshPinnedApps();
            }
        });

    }
    void showPinnedAppOptions(AppItem_Dataclass appItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(appItem.getName())
                .setItems(new CharSequence[]{"Remove Pin", "Select"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Remove Pin
                            SharedPrefHelper helper = new SharedPrefHelper(requireContext());
                            List<String> pinned = new ArrayList<>(helper.getPinnedApps());
                            pinned.remove(appItem.getPackageName());
                            helper.savePinnedApps(pinned);
                            refreshPinnedApps();
                            break;

                        case 1: // Select
                            toggleAppSelection(appItem.getPackageName());
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    public interface PinnedAppActionListener {
        void showPinnedAppOptions(AppItem_Dataclass appItem);
    }

    private void toggleAppSelection(String packageName) {
        if (selectedApps.contains(packageName)) {
            selectedApps.remove(packageName);
        } else {
            selectedApps.add(packageName);
        }
        appAdapter.notifyDataSetChanged();
    }

    void showPinnedAppOptionsSafe(AppItem_Dataclass appItem) {
        Context context = getContext();
        if (context == null || !isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Appname :"+appItem.getName()) // Cleaner title without "Appname:"
                .setItems(new CharSequence[]{"Remove Pin", "Select"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Remove Pin
                            SharedPrefHelper helper = new SharedPrefHelper(context);

                            // Get current pinned apps and remove the selected one
                            List<String> pinned = new ArrayList<>(helper.getPinnedApps());
                            pinned.remove(appItem.getPackageName());

                            // Update SharedPreferences
                            helper.setPinnedApps(pinned);

                            // Refresh UI
                            refreshPinnedApps();
                            Toast.makeText(context, "Unpinned " + appItem.getName(), Toast.LENGTH_SHORT).show();
                            break;

                        case 1: // Select
                            toggleAppSelection(appItem.getPackageName());
                            break;
                    }
                }).show();
    }

    private void setupRecyclerView(List<AppItem_Dataclass> appItems) {
        Pair<List<AppItem_Dataclass>, Integer> processed = (Pair<List<AppItem_Dataclass>, Integer>) processAppList(appItems);
        List<AppItem_Dataclass> processedList = processed.first;
        int pinnedCount = processed.second;

        if (appAdapter == null) {
            appAdapter = new AppAdapter(processedList, selectedApps, requireContext());
            appAdapter.setPinnedCount(pinnedCount); // Set pinned count
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(appAdapter);

            FastScrollView fastScrollView = requireView().findViewById(R.id.fastScrollView);
            fastScrollView.setRecyclerView(recyclerView);
            appAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    fastScrollView.setSections(appAdapter.getSections());
                }
            });
            fastScrollView.setSections(appAdapter.getSections());
        } else {
            appAdapter.updateData(processedList);
            appAdapter.setPinnedCount(pinnedCount); // Update pinned count
        }
    }

    private Pair<List<AppItem_Dataclass>, Integer> processAppList(List<AppItem_Dataclass> appItems) {
        SharedPrefHelper spHelper = new SharedPrefHelper(requireContext());
        List<String> pinnedPackageNames = spHelper.getPinnedApps();

        // Use LinkedHashMap to preserve order
        Map<String, AppItem_Dataclass> appMap = new LinkedHashMap<>();
        for (AppItem_Dataclass app : appItems) {
            appMap.put(app.getPackageName(), app);
        }

        List<AppItem_Dataclass> orderedPinned = new ArrayList<>();
        // Maintain insertion order from SharedPreferences
        for (String packageName : pinnedPackageNames) {
            if (appMap.containsKey(packageName)) {
                orderedPinned.add(appMap.remove(packageName));
            }
        }

        // Get remaining apps
        List<AppItem_Dataclass> otherApps = new ArrayList<>(appMap.values());
        Collections.sort(otherApps, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        List<AppItem_Dataclass> combined = new ArrayList<>();
        combined.addAll(orderedPinned);
        combined.addAll(otherApps);

        return new Pair<>(combined, orderedPinned.size());
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
        ImageView settingsButton = requireView().findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), SettingsActivity.class)));
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        ImageView pinButton = requireView().findViewById(R.id.pin);
        pinButton.setOnClickListener(v -> handlePinButtonClick());


        switch (currentNightMode) {
            case Configuration.UI_MODE_NIGHT_NO:
                // Light mode
              settingsButton.setImageResource(R.drawable.ic_setting_dark);
                break;

            case Configuration.UI_MODE_NIGHT_YES:
               //dark mode
                settingsButton.setImageResource(R.drawable.ic_settings);
                break;

            case Configuration.UI_MODE_NIGHT_UNDEFINED:
                // Undefined mode
                Log.d("ThemeCheck", "Theme is undefined");
                break;
        }

        AppBarLayout appBarLayout = requireView().findViewById(R.id.appBarLayout);
        TextView titleTextView = requireView().findViewById(R.id.titleTextView);
        progressBar = requireView().findViewById(R.id.progressBar);
        recyclerView = requireView().findViewById(R.id.recyclerView);
        buttonSetTime = requireView().findViewById(R.id.buttonSetTime);
        searchBar = requireView().findViewById(R.id.searchBar);
        LinearLayout buttonContainer = (LinearLayout) buttonSetTime.getParent();

        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        applyTheme(appBarLayout, titleTextView, recyclerView, buttonContainer);

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
                if (appAdapter != null) appAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }


    private void handlePinButtonClick() {
        if (selectedApps.isEmpty()) {
            Toast.makeText(requireContext(), "Select apps to pin", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(requireContext());
        sharedPrefHelper.savePinnedApps(selectedApps);
        selectedApps.clear();

        refreshPinnedApps();
    }

    // Modify refreshPinnedApps
    private void refreshPinnedApps() {
        SharedPrefHelper spHelper = new SharedPrefHelper(requireContext());
        List<String> pinnedPackageNames = spHelper.getPinnedApps();

        // Update adapter before refreshing data
        if (appAdapter != null) {
            appAdapter.setPinnedApps(pinnedPackageNames);

            // Get current list and reorder
            List<AppItem_Dataclass> currentList = new ArrayList<>(appAdapter.appListFull);
            Pair<List<AppItem_Dataclass>, Integer> processed = processAppList(currentList);

            // Animate changes
            applyListChangesWithAnimation(processed.first);
        }
    }
    private void applyListChangesWithAnimation(List<AppItem_Dataclass> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new AppDiffCallback(appAdapter.appList, newList));
        appAdapter.appList = new ArrayList<>(newList);
        diffResult.dispatchUpdatesTo(appAdapter);
    }

    // All original MainActivity2 methods below - unchanged except context access

    private void updateTimeDisplay() {
        String timeText = "";
        if (selectedDays > 0) timeText += selectedDays + "d ";
        if (selectedHours > 0) timeText += selectedHours + "h ";
        if (selectedMinutes > 0) timeText += selectedMinutes + "m";

        if (!timeText.isEmpty()) {
            buttonSetTime.setText("Lock unselected Apps (" + timeText.trim() + ")");
        }
    }

    @SuppressLint("ResourceType")
    private void showTimePickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        NumberPicker daysPicker = dialogView.findViewById(R.id.daysPicker);
        NumberPicker hoursPicker = dialogView.findViewById(R.id.hoursPicker);
        NumberPicker minutesPicker = dialogView.findViewById(R.id.minutesPicker);
        SeekBar difficultySlider = dialogView.findViewById(R.id.difficultySlider);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        TextView difficultyLabel = dialogView.findViewById(R.id.difficultyLabel);
        Button buttonSet = dialogView.findViewById(R.id.buttonSet);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonMode = dialogView.findViewById(R.id.buttonMode);
        CardView card15Min = dialogView.findViewById(R.id.card15Min);
        CardView card30Min = dialogView.findViewById(R.id.card30Min);
        CardView card1Hour = dialogView.findViewById(R.id.card1Hour);
        CardView card3Hours = dialogView.findViewById(R.id.card3Hours);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView imageView_fire=dialogView.findViewById(R.id.imageView_fire);
        //code for grew mode
        SharedPrefHelper ss=new SharedPrefHelper(getContext());
         if(ss.isGrayModeEnabled()) {
             buttonSet.setBackgroundColor(Color.parseColor("#686868"));
             // Set progress color (#FF5722 - Orange)
             difficultySlider.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#353535")));
             // Set thumb color
             difficultySlider.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#686868")));
             ColorMatrix matrix = new ColorMatrix();
             matrix.setSaturation(0); // 0 means grayscale
             ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
             imageView_fire.setColorFilter(filter);
         }else{
             buttonSet.setBackgroundColor(Color.parseColor("#FF5722"));
             // Set progress color (#FF5722 - Orange)
             difficultySlider.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF5722")));
             // Set thumb color
             difficultySlider.setThumbTintList(ColorStateList.valueOf(Color.parseColor("#FF5722")));
         }
        Glide.with(this)
                .asGif()
                .load(R.drawable.fire)
                .into(imageView_fire);











        // Set picker ranges
        daysPicker.setMinValue(0);
        daysPicker.setMaxValue(30); // Max 30 days
        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(23); // Max 23 hours
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(59); // Max 59 minutes
        // Set click listeners for preset times
        card15Min.setOnClickListener(v -> setTimeValues(1, 0, 0, daysPicker, hoursPicker, minutesPicker));
        card30Min.setOnClickListener(v -> setTimeValues(0, 0, 30, daysPicker, hoursPicker, minutesPicker));
        card1Hour.setOnClickListener(v -> setTimeValues(0, 1, 0, daysPicker, hoursPicker, minutesPicker));
        card3Hours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                challeng();
            }
        });



        // Initialize with current values
        daysPicker.setValue(selectedDays);
        hoursPicker.setValue(selectedHours);
        minutesPicker.setValue(selectedMinutes);

        // Calculate initial progress (0-100)
        int totalMinutes = (selectedDays * 24 * 60) + (selectedHours * 60) + selectedMinutes;
        int maxPossibleMinutes = (30 * 24 * 60) + (23 * 60) + 59; // 30 days, 23 hours, 59 minutes
        int progress = (int) (((float)totalMinutes / maxPossibleMinutes) * 100);
        difficultySlider.setProgress(progress);
        updateDifficultyLabel(difficultyLabel, progress);

        // Picker change listeners
        NumberPicker.OnValueChangeListener pickerChangeListener = (picker, oldVal, newVal) -> {
            int days = daysPicker.getValue();
            int hours = hoursPicker.getValue();
            int minutes = minutesPicker.getValue();

            // Calculate total minutes
            int total = (days * 24 * 60) + (hours * 60) + minutes;
            int maxTotal = (30 * 24 * 60) + (23 * 60) + 59;
            int newProgress = (int) (((float)total / maxTotal) * 100);

            // Update seekbar without triggering its listener
            difficultySlider.setOnSeekBarChangeListener(null);
            difficultySlider.setProgress(newProgress);
            updateDifficultyLabel(difficultyLabel, newProgress);
            difficultySlider.setOnSeekBarChangeListener(seekBarChangeListener);

        };

        daysPicker.setOnValueChangedListener(pickerChangeListener);
        hoursPicker.setOnValueChangedListener(pickerChangeListener);
        minutesPicker.setOnValueChangedListener(pickerChangeListener);

        // Seekbar change listener
         seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Calculate total minutes based on progress
                    int maxTotalMinutes = (30 * 24 * 60) + (23 * 60) + 59;
                    int totalMinutes = (int) ((progress / 100f) * maxTotalMinutes);

                    // Convert back to days, hours, minutes
                    int days = totalMinutes / (24 * 60);
                    int remainingMinutes = totalMinutes % (24 * 60);
                    int hours = remainingMinutes / 60;
                    int minutes = remainingMinutes % 60;

                    // Update pickers without triggering their listeners
                    daysPicker.setOnValueChangedListener(null);
                    hoursPicker.setOnValueChangedListener(null);
                    minutesPicker.setOnValueChangedListener(null);

                    daysPicker.setValue(days);
                    hoursPicker.setValue(hours);
                    minutesPicker.setValue(minutes);

                    daysPicker.setOnValueChangedListener(pickerChangeListener);
                    hoursPicker.setOnValueChangedListener(pickerChangeListener);
                    minutesPicker.setOnValueChangedListener(pickerChangeListener);

                    updateDifficultyLabel(difficultyLabel, progress);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        difficultySlider.setOnSeekBarChangeListener(seekBarChangeListener);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        AlertDialog dialog = builder.setView(dialogView).create();

        buttonSet.setOnClickListener(v -> {
            selectedDays = daysPicker.getValue();
            selectedHours = hoursPicker.getValue();
            selectedMinutes = minutesPicker.getValue();

            if(selectedApps == null || selectedApps.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least 1 App to set as mode", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedDays == 0 && selectedHours == 0 && selectedMinutes == 0) {
                Toast.makeText(requireContext(), "Please set a time limit", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isAccessibilityServiceEnabled(requireContext(), NotificationBarDetectorService.class)) {
                showAccessibilityDialog();
            } else {
                Log.d("BatteryOpt", "Already whitelisted");
                isTimeSet = true;
                updateTimeDisplay();
                dialog.dismiss();
            }
        });

        buttonMode.setOnClickListener(view -> {
            selectedDays = daysPicker.getValue();
            selectedHours = hoursPicker.getValue();
            selectedMinutes = minutesPicker.getValue();

            if (selectedDays == 0 && selectedHours == 0 && selectedMinutes == 0) {
                Toast.makeText(requireContext(), "Please set a time limit", Toast.LENGTH_SHORT).show();
                return;
            }

            if(selectedApps == null || selectedApps.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least 1 App to set as mode", Toast.LENGTH_SHORT).show();
                return;
            }

            int totalsec = ((selectedDays * 24 * 60) + (selectedHours * 60) + selectedMinutes) * 60;
            MyTileService mt = new MyTileService();
            mt.savePreferences_mode(getContext(), selectedApps, totalsec);

            Toast.makeText(requireContext(), "Mode has been set successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            new GIFDialogFragment().show(requireActivity().getSupportFragmentManager(), "gif_dialog");
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(android.view.Gravity.CENTER);
        }

        dialog.show();
    }

    private void updateDifficultyLabel(TextView label, int progress) {
        String difficulty;
        if (progress < 20) {
            difficulty = "Very Easy";
        } else if (progress < 40) {
            difficulty = "Easy";
        } else if (progress < 60) {
            difficulty = "Medium";
        } else if (progress < 80) {
            difficulty = "Hard";
        } else {
            difficulty = "Very Hard";
        }
        label.setText(difficulty);
    }
    private void challeng(){

         Dialog dialog = new ChallengeDialog(getContext());
        dialog.show();
    }

    private void showAccessibilityDialog() {
      showDisclosureDialog();
    }
    private void showDisclosureDialog() {
        // Inflate custom layout with checkbox
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.accessibility_consent_dialog, null);

        CheckBox consentCheckbox = dialogView.findViewById(R.id.consentCheckbox);
        TextView messageText = dialogView.findViewById(R.id.messageText);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Accessibility Permission Required")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Allow", null) // Set to null to override default dismissal
                .setNegativeButton("Deny", null)
                .create();

        // Custom message with explicit details
        messageText.setText("To help you manage screen time and digital wellbeing, this app needs Accessibility permission to:\n\n" +
                "• Monitor app usage\n" +
                "• Lock specific apps when time limits are reached\n" +
                "• Enforce screen time controls\n\n" +
                "By checking the box below, you confirm you understand these functions. No personal data will be collected or shared.");

        dialog.setOnShowListener(dialogInterface -> {
            Button allowButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button denybutton=dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            allowButton.setEnabled(false); // Initially disabled

            consentCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Only enable Allow button when checkbox is checked
                allowButton.setEnabled(isChecked);
            });
            denybutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            allowButton.setOnClickListener(v -> {
                if (consentCheckbox.isChecked()) {
                    dialog.dismiss();
                    openAccessibilitySettings();
                }
            });
        });

        dialog.show();
    }
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE_ACCESSIBILITY_PERMISSION);
    }
    // Helper method to update picker values
    private void setTimeValues(int days, int hours, int minutes,
                               NumberPicker daysPicker, NumberPicker hoursPicker, NumberPicker minutesPicker) {
        daysPicker.setValue(days);
        hoursPicker.setValue(hours);
        minutesPicker.setValue(minutes);
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
        boolean challengestatus =sp.getChallengeStatus(requireContext());

        if(status){
            if(challengestatus){
             startActivity(new Intent(requireContext(), challenge_reward.class));


            }else
            {
            startActivity(new Intent(requireActivity(), MainContainerActivity2.class));
            }
        }
    }

    private void loadApps() {
        PackageManager pm = requireActivity().getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppItem_Dataclass> appItems = new ArrayList<>();

        for (ApplicationInfo appInfo : apps) {
            Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
            if (launchIntent != null) {
                String appName = pm.getApplicationLabel(appInfo).toString();
                appItems.add(new AppItem_Dataclass(appName, appInfo.packageName));
            }
        }

        // Add alphabetical sorting here
        Collections.sort(appItems, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

        appAdapter = new AppAdapter(appItems, selectedApps, requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(appAdapter);
    }
    // Add these methods to MainFragment
    public boolean isTimeSet() {
        return isTimeSet;
    }

    public void resetTimeSelection() {
        isTimeSet = false;
        selectedDays = 0;
        selectedHours = 0;
        selectedMinutes = 0;
        if (buttonSetTime != null) {
            buttonSetTime.setText("Set Time");
        }
    }


    // Add these imports at the top of your class


    // Add this constant as a class member
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1001;

    // Modified function with device credential verification
    private void proceedWithSelectedTime() {
        if (selectedApps.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one app", Toast.LENGTH_SHORT).show();
            return;
        }

        // Launch device credential verification before proceeding
        launchDeviceCredentialVerification();
    }

    private void launchDeviceCredentialVerification() {
        KeyguardManager keyguardManager =
                (KeyguardManager) requireContext().getSystemService(Context.KEYGUARD_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && keyguardManager != null) {
            if (keyguardManager.isDeviceSecure()) {
                Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                        "Verify Device Lock",
                        "Confirm your PIN, pattern or password to proceed"
                );

                if (intent != null) {
                    aa.setState(Authentication.going);
                    startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);

                    return;
                }
            }
        }
        // If device credential verification is not available, proceed directly
        executeMainLogic();
    }

    // Handle the result of device credential verification
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                // Device credential verification successful
                aa.setState(Authentication.notgoing);
                Toast.makeText(requireContext(), "Authentication successful!", Toast.LENGTH_SHORT).show();
                executeMainLogic();
            } else {
                // Device credential verification failed or cancelled
                aa.setState(Authentication.notgoing);
                Toast.makeText(requireContext(), "Authentication failed or cancelled", Toast.LENGTH_SHORT).show();

            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // Extracted the main logic into a separate method
    private void executeMainLogic() {
        long totalSeconds = ((selectedDays * 24 * 60) + (selectedHours * 60) + selectedMinutes) * 60;

        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(requireContext());
        sharedPrefHelper.saveStartTime(System.currentTimeMillis());
        sharedPrefHelper.saveInitialDuration(totalSeconds);
        sharedPrefHelper.setTimeActivateStatus(true);
        sharedPrefHelper.writeData(selectedApps, totalSeconds, true);

        startActivity(new Intent(requireActivity(), MainContainerActivity2.class));
        requireActivity().finish();
    }

    private boolean isValidInput(String timeInput) {
        return !TextUtils.isEmpty(timeInput) && !timeInput.equals("0") && !selectedApps.isEmpty();
    }
    private boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        String serviceId = context.getPackageName() + "/" + service.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices == null) return false;

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            if (splitter.next().equalsIgnoreCase(serviceId)) {
                return true;
            }
        }
        return false;
    }


}
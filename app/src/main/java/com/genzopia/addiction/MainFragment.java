package com.genzopia.addiction;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public void loadAppsIfNeeded() {
        if (viewModel.getAppItems() != null) {
            setupRecyclerView(viewModel.getAppItems());
        } else {
            new LoadAppsTask().execute();
        }
    }
    private void setupRecyclerView(List<AppItem_Dataclass> appItems) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        appAdapter = new AppAdapter(appItems, selectedApps, requireContext());
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
    }

    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppItem_Dataclass>> {
        @Override
        protected List<AppItem_Dataclass> doInBackground(Void... voids) {
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
            Collections.sort(appItems, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            return appItems;
        }

        @Override
        protected void onPostExecute(List<AppItem_Dataclass> appItems) {
            if (isAdded()) {
                viewModel.setAppItems(appItems);
                setupRecyclerView(appItems);
            }
        }
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

    private void showTimePickerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        NumberPicker daysPicker = dialogView.findViewById(R.id.daysPicker);
        NumberPicker hoursPicker = dialogView.findViewById(R.id.hoursPicker);
        NumberPicker minutesPicker = dialogView.findViewById(R.id.minutesPicker);
        Button buttonSet = dialogView.findViewById(R.id.buttonSet);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonmode=dialogView.findViewById(R.id.buttonMode);

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
//doing work
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

                AlertDialog.Builder builderr = new AlertDialog.Builder(getContext());
                builderr.setTitle("Accessibility Permission Required");
                builderr.setMessage("To completely lock the apps, this app needs Accessibility feature to be enabled. Please enable the feature.");

                builderr.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });

                builderr.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getContext(), "You can't lock apps without Accessibility access", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                });

                AlertDialog dialoge = builderr.create();
                dialoge.show();
            }else {
                Log.d("BatteryOpt", "Already whitelisted");
                isTimeSet = true;
                updateTimeDisplay();
                dialog.dismiss();

            }

        });
        buttonmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                // Show GIF Dialog
                new GIFDialogFragment().show(requireActivity().getSupportFragmentManager(), "gif_dialog");
            }
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
            startActivity(new Intent(requireActivity(), MainContainerActivity2.class));
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


    private void proceedWithSelectedTime() {
        if (selectedApps.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one app", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalsec = ((selectedDays * 24 * 60) + (selectedHours * 60) + selectedMinutes)*60;
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(requireContext());
        sharedPrefHelper.writeData(selectedApps, totalsec, true);

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
package com.genzopia.addiction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText searchBar;
    private Button buttonProceed;
    private AppAdapter appAdapter;
    private ArrayList<String> selectedApps = new ArrayList<>();
    private EditText time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Initialize views
        searchBar = findViewById(R.id.searchBar);
        recyclerView = findViewById(R.id.recyclerView);
        time = findViewById(R.id.time);
        buttonProceed = findViewById(R.id.buttonProceed);
        loadApps();
        checksp();

        // Proceed button to go to MainActivity3
        buttonProceed.setOnClickListener(v -> {
            String timeInput = time.getText().toString().trim();

            if (isValidInput(timeInput)) {
                SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(this);
                sharedPrefHelper.writeData(selectedApps, Integer.parseInt(timeInput)*60, true);

                // Start MainActivity3
                Intent intent = new Intent(MainActivity2.this, MainActivity3.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Please enter a valid time and select at least one app", Toast.LENGTH_SHORT).show();
            }
        });


        // Search functionality
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                appAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
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




    private boolean isValidInput(String timeInput) {
        return !TextUtils.isEmpty(timeInput) && !timeInput.equals("0") && !selectedApps.isEmpty();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {

    }
}

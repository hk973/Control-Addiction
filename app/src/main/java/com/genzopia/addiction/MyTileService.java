package com.genzopia.addiction;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MyTileService extends TileService {

    private static final String PREFS_NAME = "TilePrefs";
    private static final String KEY_TILE_NAME = "TileName";
    private static final String KEY_TILE_ICON = "TileIcon";
    private static final String KEY_TILE_STATE = "TileState";

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load stored tile state
        int savedState = prefs.getInt(KEY_TILE_STATE, Tile.STATE_INACTIVE);
        String tileName = prefs.getString(KEY_TILE_NAME, "Mode 1");
        String iconPath = prefs.getString(KEY_TILE_ICON, null);

        Tile tile = getQsTile();
        tile.setLabel(tileName);
        tile.setState(savedState);

        // Update icon
        if (iconPath != null) {
            Icon icon = Icon.createWithContentUri(iconPath);
            tile.setIcon(icon);
        } else {
            tile.setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher_foreground));
        }

        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        final Tile tile = getQsTile();
        if (tile == null) return;

        // Set tile to active state immediately
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();

        // Save active state
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_TILE_STATE, Tile.STATE_ACTIVE).apply();

        // Start async operation
        new Thread(() -> {
            try {

                start_mode();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Update UI on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Tile updatedTile = getQsTile();
                if (updatedTile != null) {
                    // Set tile to inactive state
                    updatedTile.setState(Tile.STATE_INACTIVE);
                    updatedTile.updateTile();

                    // Save inactive state
                    SharedPreferences prefs1 = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs1.edit().putInt(KEY_TILE_STATE, Tile.STATE_INACTIVE).apply();
                }
            });
        }).start();
    }
    // Method to save data to SharedPreferences
    public  void savePreferences_mode(Context context,
                                       ArrayList<String> selectedAppMode,
                                       int secMode) {
        SharedPreferences sharedPref = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // Convert ArrayList to Set
        Set<String> set = new HashSet<>(selectedAppMode);

        // Save both values
        editor.putStringSet("selected_app_mode", set);
        editor.putInt("sec_mode", secMode);

        editor.apply();
    }

    // Method to retrieve and use the saved data
    public void start_mode() {
       ArrayList<String>savedApps=new ArrayList<>();
       savedApps=getSelectedAppMode(this);
       int savedSecMode=getSecMode(this);
       Log.e("test333", String.valueOf(savedSecMode));

        // If you still need to use SharedPrefHelper (though not clear why since we already have the values)
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(getBaseContext());
        sharedPrefHelper.saveStartTime(System.currentTimeMillis());
        sharedPrefHelper.saveInitialDuration(savedSecMode);
        sharedPrefHelper.setTimeActivateStatus(true);
        sharedPrefHelper.writeData(savedApps, savedSecMode, true);

        // Start activity with the retrieved values
        Intent intent = new Intent(this, MainContainerActivity2.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public ArrayList<String> getSelectedAppMode(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE);
        // Retrieve set with empty default
        Set<String> set = sharedPref.getStringSet("selected_app_mode", new HashSet<>());
        // Convert to ArrayList
        return new ArrayList<>(set);
    }

    public int getSecMode(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE);
        // Retrieve integer with 0 as default
        return sharedPref.getInt("sec_mode", 0);
    }
}
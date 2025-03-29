package com.genzopia.addiction;



import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class MyTileService extends TileService {

    private static final String PREFS_NAME = "TilePrefs";
    private static final String KEY_TILE_NAME = "TileName";
    private static final String KEY_TILE_ICON = "TileIcon";

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load stored tile name and icon
        String tileName = prefs.getString(KEY_TILE_NAME, "Mode 1");
        String iconPath = prefs.getString(KEY_TILE_ICON, null);

        Tile tile = getQsTile();
        tile.setLabel(tileName);
        tile.setState(Tile.STATE_ACTIVE);

        // If an icon is selected, update it dynamically
        if (iconPath != null) {
            Icon icon = Icon.createWithContentUri(iconPath);
            tile.setIcon(icon);
        } else {
            tile.setIcon(Icon.createWithResource(this, R.drawable.dot_selected));
        }

        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pendingIntent);
        }
    }
}

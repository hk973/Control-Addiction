package com.genzopia.addiction.data.model;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/**
 * Immutable description of a launchable application.
 * The icon is resolved lazily so a whole app list can be kept in memory cheaply.
 */
public class AppInfo {

    private final String label;
    private final String packageName;
    private final PackageManager packageManager;
    private Drawable icon;

    public AppInfo(String label, String packageName) {
        this(label, packageName, null);
    }

    public AppInfo(String label, String packageName, PackageManager packageManager) {
        this.label = label;
        this.packageName = packageName;
        this.packageManager = packageManager;
    }

    public String getLabel() {
        return label;
    }

    public String getPackageName() {
        return packageName;
    }

    /** Resolves the launcher icon on first call. Returns null when it cannot be loaded. */
    public Drawable getIcon() {
        if (icon == null && packageManager != null) {
            try {
                icon = packageManager.getApplicationIcon(packageName);
            } catch (Exception e) {
                icon = null;
            }
        }
        return icon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppInfo)) return false;
        AppInfo other = (AppInfo) o;
        return packageName == null ? other.packageName == null : packageName.equals(other.packageName);
    }

    @Override
    public int hashCode() {
        return packageName == null ? 0 : packageName.hashCode();
    }

    @Override
    public String toString() {
        return label + " (" + packageName + ")";
    }
}

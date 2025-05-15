package com.genzopia.addiction;

import android.graphics.drawable.Drawable;

public class AppItem {
    private String appName;
    private String packageName;
    private Drawable icon;

    public AppItem(String appName, String packageName, Drawable icon) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }
}

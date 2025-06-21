// PermissionListener.java
package com.genzopia.addiction.Launcher.permission;

public interface PermissionListener {
    void onPermissionGranted(int position);
    void onAllPermissionsComplete();
}


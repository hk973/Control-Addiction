// PermissionListener.java
package com.genzopia.addiction.permission;

public interface PermissionListener {
    void onPermissionGranted(int position);
    void onAllPermissionsComplete();
}


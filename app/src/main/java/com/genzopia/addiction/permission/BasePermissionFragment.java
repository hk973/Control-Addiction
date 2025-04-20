// BasePermissionFragment.java
package com.genzopia.addiction.permission;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public abstract class BasePermissionFragment extends Fragment {
    protected PermissionListener listener;
    protected int position;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof PermissionListener) {
            listener = (PermissionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PermissionListener");
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkPermissionStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermissionStatus();
    }

    protected abstract void checkPermissionStatus();

    public abstract boolean isPermissionGranted();

    protected void notifyPermissionGranted() {
        if (listener != null) {
            listener.onPermissionGranted(position);
        }
    }
}
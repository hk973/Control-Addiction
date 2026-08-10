package com.genzopia.addiction.Launcher;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.genzopia.addiction.R;
import com.genzopia.addiction.data.AppRepository;
import com.genzopia.addiction.data.model.AppInfo;
import com.genzopia.addiction.ui.common.AppListAdapter;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Home-screen shortcut: opens the app the user assigned, or shows a picker.
 * The installed-app list comes from {@link AppRepository} so it is shared with the drawer.
 */
public class PopupSelectApp {

    private static final String KEY_SHORTCUT = "shortcut";

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ShortcutPicker");
        thread.setDaemon(true);
        return thread;
    });

    private final Context context;
    private final SharedPrefHelper prefHelper;
    private final AppRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PopupSelectApp(Context context) {
        this.context = context;
        this.prefHelper = new SharedPrefHelper(context);
        this.repository = AppRepository.getInstance(context);
    }

    /** Opens the saved shortcut; asks the user to pick one when nothing is saved yet. */
    public void show(ImageView shortcutButton) {
        String saved = savedShortcut();
        if (TextUtils.isEmpty(saved)) {
            showPicker(shortcutButton);
        } else {
            openApp(saved);
        }
    }

    /** Same as {@link #show} but never allows assigning a new shortcut while locked. */
    public void showlock(ImageView shortcutButton) {
        String saved = savedShortcut();
        if (TextUtils.isEmpty(saved)) {
            Toast.makeText(context, "You cannot set Shortcut from lock screen", Toast.LENGTH_SHORT).show();
        } else {
            openApp(saved);
        }
    }

    /** Always shows the picker, used to reassign the shortcut on long press. */
    public void show2(ImageView shortcutButton) {
        showPicker(shortcutButton);
    }

    private String savedShortcut() {
        // NOTE: this used to compare the stored value with ==, so a saved shortcut was
        // never recognised and the picker opened every single time.
        return prefHelper.getString(context, KEY_SHORTCUT, "");
    }

    private void showPicker(ImageView shortcutButton) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_app_selector);

        RecyclerView recyclerView = dialog.findViewById(R.id.recyclerViewApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        AppListAdapter adapter = new AppListAdapter(context, new AppListAdapter.Config()
                .showIcon(true)
                .onClick(app -> {
                    Toast.makeText(context, "Appname :" + app.getLabel(), Toast.LENGTH_SHORT).show();
                    if (app.getIcon() != null) {
                        shortcutButton.setImageDrawable(app.getIcon());
                    }
                    dialog.dismiss();
                    EXECUTOR.execute(() -> prefHelper.saveString(context, KEY_SHORTCUT, app.getPackageName()));
                }));
        recyclerView.setAdapter(adapter);

        List<AppInfo> cached = repository.getCachedApps();
        if (!cached.isEmpty()) {
            adapter.submitAppList(cached);
        } else {
            // First launch: the cache is not filled yet, so load it off the main thread.
            EXECUTOR.execute(() -> {
                List<AppInfo> apps = repository.getAppsBlocking();
                mainHandler.post(() -> {
                    if (dialog.isShowing()) {
                        adapter.submitAppList(apps);
                    }
                });
            });
        }

        dialog.show();
    }

    private boolean openApp(String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent == null) {
                // Installed but without a launcher activity, or uninstalled meanwhile.
                Toast.makeText(context, "App doesn't have a launcher activity", Toast.LENGTH_SHORT).show();
                return false;
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            return true;
        } catch (Exception e) {
            Toast.makeText(context, "App not installed", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}

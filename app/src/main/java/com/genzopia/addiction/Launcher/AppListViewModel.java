package com.genzopia.addiction.Launcher;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.genzopia.addiction.data.AppRepository;
import com.genzopia.addiction.data.model.AppInfo;

import java.util.List;

/**
 * Thin bridge between the UI and {@link AppRepository}. The repository owns the cache
 * and keeps it in sync with package install/uninstall broadcasts, so the ViewModel only
 * forwards the LiveData and the refresh request.
 */
public class AppListViewModel extends AndroidViewModel {

    private final AppRepository repository;

    public AppListViewModel(@NonNull Application application) {
        super(application);
        this.repository = AppRepository.getInstance(application);
    }

    /** Kept for compatibility with the existing call sites; the context is ignored. */
    public void loadApps(Context context) {
        repository.refresh();
    }

    public void refresh() {
        repository.refresh();
    }

    public LiveData<List<AppInfo>> getAppItemsLiveData() {
        return repository.getApps();
    }
}

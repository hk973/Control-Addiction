package com.genzopia.addiction;

import androidx.lifecycle.ViewModel;
import java.util.List;

public class AppListViewModel extends ViewModel {
    private List<AppItem_Dataclass> appItems;

    public List<AppItem_Dataclass> getAppItems() {
        return appItems;
    }

    public void setAppItems(List<AppItem_Dataclass> appItems) {
        this.appItems = appItems;
    }
}
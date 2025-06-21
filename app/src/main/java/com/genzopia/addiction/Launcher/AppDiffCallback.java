package com.genzopia.addiction.Launcher;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

class AppDiffCallback extends DiffUtil.Callback {
    private final List<AppItem_Dataclass> oldList;
    private final List<AppItem_Dataclass> newList;

    public AppDiffCallback(List<AppItem_Dataclass> oldList, List<AppItem_Dataclass> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override public int getOldListSize() { return oldList.size(); }
    @Override public int getNewListSize() { return newList.size(); }

    @Override
    public boolean areItemsTheSame(int oldPos, int newPos) {
        return oldList.get(oldPos).getPackageName().equals(newList.get(newPos).getPackageName());
    }

    @Override
    public boolean areContentsTheSame(int oldPos, int newPos) {
        return oldList.get(oldPos).equals(newList.get(newPos));
    }
}
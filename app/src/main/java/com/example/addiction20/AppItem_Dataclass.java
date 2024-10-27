package com.example.addiction20;

public class AppItem_Dataclass {
    private String name; // Application name
    private String packageName; // Application package name

    // Constructor
    public AppItem_Dataclass(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }
}

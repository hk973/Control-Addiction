package com.genzopia.addiction.Launcher;

import java.util.ArrayList;

public class AuthenticationManager {
    private static AuthenticationManager instance;
    private Authentication currentState = Authentication.notgoing;
    private final ArrayList<AuthenticationListener> listeners = new ArrayList<>();

    public interface AuthenticationListener {
        void onStateChanged(Authentication newState);
    }

    private AuthenticationManager() {}

    public static synchronized AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager();
        }
        return instance;
    }

    public void setState(Authentication newState) {
        if (currentState != newState) {
            currentState = newState;
            notifyListeners();
        }
    }

    public Authentication getState() {
        return currentState;
    }

    public void addListener(AuthenticationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AuthenticationListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (AuthenticationListener listener : listeners) {
            listener.onStateChanged(currentState);
        }
    }
}
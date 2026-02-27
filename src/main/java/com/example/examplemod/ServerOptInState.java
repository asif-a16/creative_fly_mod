package com.example.examplemod;

public final class ServerOptInState {
    private static boolean optedIn;

    private ServerOptInState() {
    }

    public static boolean isOptedIn() {
        return optedIn;
    }

    public static void setOptedIn(boolean enabled) {
        optedIn = enabled;
    }
}

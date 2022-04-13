package com.luixtech.rpc.core.utils;

public abstract class DebugModeHolder {
    private static boolean debugMode = false;

    public static void setDebugMode(boolean mode) {
        debugMode = mode;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }
}

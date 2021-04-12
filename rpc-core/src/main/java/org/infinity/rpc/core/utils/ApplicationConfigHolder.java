package org.infinity.rpc.core.utils;

import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.utilities.concurrent.ThreadSafe;

@ThreadSafe
public abstract class ApplicationConfigHolder {
    private static ApplicationConfig cache;

    public static void set(ApplicationConfig config) {
        cache = config;
    }

    public static ApplicationConfig get() {
        return cache;
    }
}

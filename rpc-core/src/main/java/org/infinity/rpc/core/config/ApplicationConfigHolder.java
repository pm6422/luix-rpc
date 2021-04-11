package org.infinity.rpc.core.config;

import org.infinity.rpc.utilities.concurrent.ThreadSafe;

@ThreadSafe
public abstract class ApplicationConfigHolder {
    private static ApplicationExtConfig cache;

    public static void set(ApplicationExtConfig config) {
        cache = config;
    }

    public static ApplicationExtConfig get() {
        return cache;
    }
}

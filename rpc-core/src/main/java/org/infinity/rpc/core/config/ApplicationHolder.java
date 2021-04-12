package org.infinity.rpc.core.config;

import org.infinity.rpc.utilities.concurrent.ThreadSafe;

@ThreadSafe
public abstract class ApplicationHolder {
    private static ApplicationConfig cache;

    public static void set(ApplicationConfig config) {
        cache = config;
    }

    public static ApplicationConfig get() {
        return cache;
    }
}

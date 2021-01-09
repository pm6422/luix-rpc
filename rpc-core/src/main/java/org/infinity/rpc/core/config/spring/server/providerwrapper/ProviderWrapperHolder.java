package org.infinity.rpc.core.config.spring.server.providerwrapper;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class ProviderWrapperHolder {
    /**
     * RPC provider wrapper map
     */
    private final Map<String, ProviderWrapper<?>> wrapperCache = new ConcurrentHashMap<>();

    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderWrapperHolder() {
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link ProviderWrapperHolder}
     */
    public static ProviderWrapperHolder getInstance() {
        return ProviderWrapperHolder.SingletonHolder.INSTANCE;
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        private static final ProviderWrapperHolder INSTANCE = new ProviderWrapperHolder();// static variable will be instantiated on class loading.
    }

    public synchronized Map<String, ProviderWrapper<?>> getWrappers() {
        return wrapperCache;
    }

    public synchronized void addWrapper(String name, ProviderWrapper<?> providerWrapper) {
        wrapperCache.putIfAbsent(name, providerWrapper);
    }
}

package org.infinity.rpc.core.server.stub;

import org.infinity.rpc.utilities.concurrent.ThreadSafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class ProviderStubHolder {
    /**
     * RPC provider stub map
     */
    private final Map<String, ProviderStub<?>> stubCache = new ConcurrentHashMap<>();

    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderStubHolder() {
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link ProviderStubHolder}
     */
    public static ProviderStubHolder getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public synchronized Map<String, ProviderStub<?>> getStubs() {
        return stubCache;
    }

    public synchronized void addStub(String name, ProviderStub<?> providerStub) {
        stubCache.putIfAbsent(name, providerStub);
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        /**
         * Static variable will be instantiated on class loading.
         */
        private static final ProviderStubHolder INSTANCE = new ProviderStubHolder();
    }
}

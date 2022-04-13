package com.luixtech.luixrpc.core.server.stub;

import com.luixtech.luixrpc.utilities.concurrent.ThreadSafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class ProviderStubHolder {

    private static final ProviderStubHolder           INSTANCE = new ProviderStubHolder();
    /**
     * RPC provider stub map
     */
    private final        Map<String, ProviderStub<?>> cache    = new ConcurrentHashMap<>();

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
        return INSTANCE;
    }

    public synchronized void add(String name, ProviderStub<?> providerStub) {
        cache.putIfAbsent(name, providerStub);
    }

    public synchronized Map<String, ProviderStub<?>> getMap() {
        return cache;
    }
}

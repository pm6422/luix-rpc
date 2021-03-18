package org.infinity.rpc.core.client.stub;

import org.infinity.rpc.utilities.concurrent.ThreadSafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class ConsumerStubHolder {
    /**
     * RPC provider stub map
     */
    private final Map<String, ConsumerStub<?>> stubCache = new ConcurrentHashMap<>();

    /**
     * Prevent instantiation of it outside the class
     */
    private ConsumerStubHolder() {
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link ConsumerStubHolder}
     */
    public static ConsumerStubHolder getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public synchronized Map<String, ConsumerStub<?>> getStubs() {
        return stubCache;
    }

    public synchronized void addStub(String name, ConsumerStub<?> consumerStub) {
        stubCache.putIfAbsent(name, consumerStub);
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        /**
         * Static variable will be instantiated on class loading.
         */
        private static final ConsumerStubHolder INSTANCE = new ConsumerStubHolder();
    }
}

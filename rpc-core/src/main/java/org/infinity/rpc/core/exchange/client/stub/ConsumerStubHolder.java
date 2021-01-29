package org.infinity.rpc.core.exchange.client.stub;

import org.infinity.rpc.core.exchange.server.stub.ProviderStub;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class ConsumerStubHolder {
    /**
     * RPC provider stub map
     */
    private final List<ConsumerStub<?>> stubCache = new ArrayList<>();

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

    public synchronized List<ConsumerStub<?>> getStubs() {
        return stubCache;
    }

    public synchronized void addStub(ConsumerStub<?> consumerStub) {
        stubCache.add(consumerStub);
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

package org.infinity.rpc.core.exchange.cluster;

import org.apache.commons.collections4.MapUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class ProviderClusterHolder<T> {
    /**
     * Cluster map
     */
    private final Map<String, ProviderCluster<T>> clusterMap = new ConcurrentHashMap<>();

    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderClusterHolder() {
    }

    public synchronized boolean empty() {
        return MapUtils.isEmpty(clusterMap);
    }

    public synchronized List<ProviderCluster<T>> getClusters() {
        return new ArrayList<>(clusterMap.values());
    }

    public synchronized void addCluster(String name, ProviderCluster<T> providerCluster) {
        clusterMap.putIfAbsent(name, providerCluster);
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link ProviderClusterHolder}
     */
    @SuppressWarnings({"rawtypes"})
    public static ProviderClusterHolder getInstance() {
        return ProviderClusterHolder.SingletonHolder.INSTANCE;
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        // static variable will be instantiated on class loading.
        @SuppressWarnings({"rawtypes"})
        private static final ProviderClusterHolder INSTANCE = new ProviderClusterHolder();
    }
}

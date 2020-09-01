package org.infinity.rpc.core.exchange.cluster;

import org.apache.commons.collections4.MapUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ThreadSafe
public class ClusterHolder<T> {
    /**
     * Cluster map
     */
    private final Map<String, Cluster<T>> clusterMap = new ConcurrentHashMap<>();

    /**
     * Prohibit instantiate an instance outside the class
     */
    private ClusterHolder() {
    }

    public synchronized boolean empty() {
        return MapUtils.isEmpty(clusterMap);
    }

    public synchronized List<Cluster<T>> getClusters() {
        return clusterMap.values().stream().collect(Collectors.toList());
    }

    public synchronized void addCluster(String name, Cluster<T> cluster) {
        clusterMap.putIfAbsent(name, cluster);
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link ClusterHolder}
     */
    public static ClusterHolder getInstance() {
        return ClusterHolder.SingletonHolder.INSTANCE;
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        private static final ClusterHolder INSTANCE = new ClusterHolder();// static variable will be instantiated on class loading.
    }
}

package org.infinity.luix.utilities.statistics;

import com.codahale.metrics.MetricRegistry;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class CachedMetricsFactory {
    private static final MetricRegistry                        DEFAULT_METRIC_REGISTRY = new MetricRegistry();
    private static final String                                METRIC_REGISTRY_DEFAULT = "default";
    private static final ConcurrentMap<String, MetricRegistry> METRIC_REGISTRIES       = new ConcurrentHashMap<>();

    static {
        METRIC_REGISTRIES.put(METRIC_REGISTRY_DEFAULT, DEFAULT_METRIC_REGISTRY);
    }

    /**
     * Get {@link MetricRegistry} instance associated with the name
     *
     * @param name name of {@link MetricRegistry}
     * @return {@link MetricRegistry} instance
     */
    public static MetricRegistry getRegistryInstance(String name) {
        MetricRegistry instance = METRIC_REGISTRIES.get(name);
        if (instance == null) {
            METRIC_REGISTRIES.putIfAbsent(name, new MetricRegistry());
            instance = METRIC_REGISTRIES.get(name);
        }
        return instance;
    }

    /**
     * Get {@link MetricRegistry} instance associated with the multiple names
     *
     * @param name  the first element of the name
     * @param names the remaining elements of the name
     * @return {@link MetricRegistry} instance
     */
    public static MetricRegistry getRegistryInstance(String name, String... names) {
        // Concatenates elements to form a dotted name
        String key = MetricRegistry.name(name, names);
        MetricRegistry instance = METRIC_REGISTRIES.get(key);
        if (instance == null) {
            METRIC_REGISTRIES.putIfAbsent(key, new MetricRegistry());
            instance = METRIC_REGISTRIES.get(name);
        }
        return instance;
    }

    /**
     * Get {@link MetricRegistry} instance associated with the class and names
     *
     * @param clazz class
     * @param names names
     * @return {@link MetricRegistry} instance
     */
    public static MetricRegistry getRegistryInstance(Class<?> clazz, String... names) {
        // Concatenates elements to form a dotted name
        String key = MetricRegistry.name(clazz, names);
        MetricRegistry instance = METRIC_REGISTRIES.get(key);
        if (instance == null) {
            METRIC_REGISTRIES.putIfAbsent(key, new MetricRegistry());
            instance = METRIC_REGISTRIES.get(key);
        }
        return instance;
    }

    /**
     * Get default {@link MetricRegistry} instance
     */
    public static MetricRegistry getDefaultMetricRegistry() {
        return DEFAULT_METRIC_REGISTRY;
    }

    /**
     * Get all {@link MetricRegistry} instances
     */
    public static Map<String, MetricRegistry> allRegistries() {
        return Collections.unmodifiableMap(METRIC_REGISTRIES);
    }
}
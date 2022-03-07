package org.infinity.luix.metrics.statistic;

import com.codahale.metrics.MetricRegistry;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class CachedMetricsFactory {
    private static final MetricRegistry                        DEFAULT_METRIC_REGISTRY = new MetricRegistry();
    private static final String                                DEFAULT_REGISTRY_KEY    = "default";
    private static final ConcurrentMap<String, MetricRegistry> METRIC_REGISTRIES       = new ConcurrentHashMap<>();

    static {
        METRIC_REGISTRIES.put(DEFAULT_REGISTRY_KEY, DEFAULT_METRIC_REGISTRY);
    }

    /**
     * Get {@link MetricRegistry} instance associated with the multiple names
     *
     * @param name  the first element of the name
     * @param names the remaining elements of the name
     * @return {@link MetricRegistry} instance
     */
    public static MetricRegistry getMetricsRegistry(String name, String... names) {
        // Concatenates elements to form a dotted name
        String key = MetricRegistry.name(name, names);
        return getMetricsRegistry(key);
    }

    /**
     * Get {@link MetricRegistry} instance associated with the class and names
     *
     * @param clazz class
     * @param names names
     * @return {@link MetricRegistry} instance
     */
    public static MetricRegistry getMetricsRegistry(Class<?> clazz, String... names) {
        // Concatenates elements to form a dotted name
        String key = MetricRegistry.name(clazz, names);
        return getMetricsRegistry(key);
    }

    /**
     * Get {@link MetricRegistry} instance associated with the name
     *
     * @param name name of {@link MetricRegistry}
     * @return {@link MetricRegistry} instance
     */
    public static MetricRegistry getMetricsRegistry(String name) {
        METRIC_REGISTRIES.putIfAbsent(name, new MetricRegistry());
        return METRIC_REGISTRIES.get(name);
    }

    /**
     * Get default {@link MetricRegistry} instance
     *
     * @return default {@link MetricRegistry} instance
     */
    public static MetricRegistry getDefaultMetricRegistry() {
        return DEFAULT_METRIC_REGISTRY;
    }

    /**
     * Get all {@link MetricRegistry} instances
     *
     * @return all {@link MetricRegistry} instances
     */
    public static Map<String, MetricRegistry> getAllRegistries() {
        return Collections.unmodifiableMap(METRIC_REGISTRIES);
    }
}
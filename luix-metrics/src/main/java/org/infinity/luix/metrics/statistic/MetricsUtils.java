package org.infinity.luix.metrics.statistic;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.metrics.statistic.access.CallMetric;
import org.infinity.luix.metrics.statistic.access.Metric;
import org.infinity.luix.metrics.statistic.access.ResponseType;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class MetricsUtils {
    /**
     * Call interval in seconds
     */
    public static final  int                               SCHEDULED_STATISTIC_INTERVAL = 30;
    private static final ConcurrentHashMap<String, Metric> METRICS_CACHE                = new ConcurrentHashMap<>();

    public static String getMemoryStatistic() {
        Runtime runtime = Runtime.getRuntime();
        // Unit: MB
        double totalMemory = (double) runtime.totalMemory() / (1024 * 1024);
        double freeMemory = (double) runtime.freeMemory() / (1024 * 1024);
        double maxMemory = (double) runtime.maxMemory() / (1024 * 1024);

        double usedMemory = totalMemory - freeMemory;
        double freePercentage = ((maxMemory - usedMemory) / maxMemory) * 100.0;
        double usedPercentage = 100 - freePercentage;

        DecimalFormat sizeFormat = new DecimalFormat("#0.00");
        DecimalFormat percentageFormat = new DecimalFormat("#0.0");

        return sizeFormat.format(usedMemory) + "MB of " + sizeFormat.format(maxMemory) + " MB (" +
                percentageFormat.format(usedPercentage) + "%) used";
    }

    public static void trackCall(String name, long timestamp, long processingTime, long bizProcessingTime,
                                 int slowThreshold, ResponseType responseType) {
        Validate.notNull(name, "name cannot be null");
        Metric metric = getMetric(name, timestamp);
        metric.track(timestamp, processingTime, bizProcessingTime, slowThreshold, responseType);
    }

    private static Metric getMetric(String name, long timestamp) {
        Metric metric = METRICS_CACHE.get(name);
        if (metric == null) {
            METRICS_CACHE.putIfAbsent(name, new Metric(name, timestamp));
            metric = METRICS_CACHE.get(name);
        }
        return metric;
    }

    public static ConcurrentHashMap<String, CallMetric> getAllCallMetrics() {
        ConcurrentHashMap<String, CallMetric> callMetrics = new ConcurrentHashMap<>();
        for (Map.Entry<String, Metric> entry : METRICS_CACHE.entrySet()) {
            CallMetric callMetric = getCallMetric(callMetrics, entry.getKey());
            CallMetric currentCallMetric = entry.getValue().getCallMetric(System.currentTimeMillis(), SCHEDULED_STATISTIC_INTERVAL);
            callMetric.increment(currentCallMetric);
        }
        return callMetrics;
    }

    private static CallMetric getCallMetric(Map<String, CallMetric> callMetrics, String key) {
        CallMetric callMetric = callMetrics.get(key);
        if (callMetric == null) {
            callMetrics.putIfAbsent(key, new CallMetric());
            callMetric = callMetrics.get(key);
        }
        return callMetric;
    }
}

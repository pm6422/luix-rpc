package org.infinity.luix.metrics.statistic;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.metrics.statistic.access.CallMetric;
import org.infinity.luix.metrics.statistic.access.Metric;
import org.infinity.luix.metrics.statistic.access.ResponseType;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class MetricsUtils {
    /**
     * Access statistic interval in seconds
     */
    public static final  int                           SCHEDULED_STATISTIC_INTERVAL     = 30;
    public static        String                        DELIMITER                        = "\\|";
    public static final  String                        PROCESSING_TIME_METRICS_REGISTRY = "defaultProcessingTime";
    public static final  String                        PROCESSING_TIME_HISTOGRAM        = MetricRegistry.name(Metric.class, "processingTime");
    private static final ConcurrentMap<String, Metric> METRICS_CACHE                    = new ConcurrentHashMap<>();

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
        METRICS_CACHE.putIfAbsent(name, new Metric(name, timestamp));
        return METRICS_CACHE.get(name);
    }

    public static ConcurrentMap<String, CallMetric> getAllCallMetrics() {
        ConcurrentMap<String, CallMetric> callMetrics = new ConcurrentHashMap<>();
        for (Map.Entry<String, Metric> entry : METRICS_CACHE.entrySet()) {
            Metric metric = entry.getValue();
            CallMetric callMetric = metric.getCallMetric(System.currentTimeMillis(), SCHEDULED_STATISTIC_INTERVAL);
            callMetrics.putIfAbsent(entry.getKey(), new CallMetric());

            CallMetric appResult = callMetrics.get(entry.getKey());
            appResult.setProcessingTime(appResult.getProcessingTime() + callMetric.getProcessingTime());
            appResult.setBizProcessingTime(appResult.getBizProcessingTime() + callMetric.getBizProcessingTime());
            appResult.setAccessCount(appResult.getAccessCount() + callMetric.getAccessCount());
            appResult.setSlowExecutionCount(appResult.getSlowExecutionCount() + callMetric.getSlowExecutionCount());
            appResult.setBizExceptionCount(appResult.getBizExceptionCount() + callMetric.getBizExceptionCount());
            appResult.setOtherExceptionCount(appResult.getOtherExceptionCount() + callMetric.getOtherExceptionCount());
        }
        return callMetrics;
    }
}

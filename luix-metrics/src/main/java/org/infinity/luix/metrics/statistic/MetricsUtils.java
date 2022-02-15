package org.infinity.luix.metrics.statistic;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.metrics.statistic.access.AccessMetrics;
import org.infinity.luix.metrics.statistic.access.AccessResult;
import org.infinity.luix.metrics.statistic.access.StatisticType;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class MetricsUtils {
    /**
     * Access statistic interval in seconds
     */
    public static final  int                                  SCHEDULED_STATISTIC_INTERVAL = 30;
    public static        String                               DELIMITER                    = "\\|";
    public static final  String                               ELAPSED_TIME_HISTOGRAM       = MetricRegistry.name(AccessMetrics.class, "elapsedTime");
    private static final ConcurrentMap<String, AccessMetrics> ACCESS_STATISTICS            = new ConcurrentHashMap<>();

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

    public static void logAccess(String name, String application, String module,
                                 long currentTimeMillis, long elapsedTimeMillis, long bizProcessTime, int slowThreshold,
                                 StatisticType statisticType) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        try {
            AccessMetrics item = getStatisticItem(name + "|" + application + "|" + module, currentTimeMillis);
            item.save(currentTimeMillis, elapsedTimeMillis, bizProcessTime, slowThreshold, statisticType);
        } catch (Exception e) {
            log.error("Failed to log access!", e);
        }
    }

    private static AccessMetrics getStatisticItem(String name, long currentTimeMillis) {
        AccessMetrics item = ACCESS_STATISTICS.get(name);
        if (item == null) {
            ACCESS_STATISTICS.put(name, new AccessMetrics(name, currentTimeMillis));
            item = ACCESS_STATISTICS.get(name);
        }
        return item;
    }

    public static ConcurrentMap<String, AccessResult> getTotalAccessStatistic() {
        ConcurrentMap<String, AccessResult> totalResults = new ConcurrentHashMap<>();
        for (Map.Entry<String, AccessMetrics> entry : ACCESS_STATISTICS.entrySet()) {
            AccessMetrics item = entry.getValue();
            AccessResult result = item.getStatisticResult(System.currentTimeMillis(), SCHEDULED_STATISTIC_INTERVAL);

            String key = entry.getKey();
            String[] keys = key.split(DELIMITER);
            if (keys.length != 3) {
                continue;
            }
            String application = keys[1];
            String module = keys[2];
            key = application + "|" + module;
            AccessResult appResult = totalResults.get(key);
            if (appResult == null) {
                totalResults.putIfAbsent(key, new AccessResult());
                appResult = totalResults.get(key);
            }

            appResult.setProcessingTime(appResult.getProcessingTime() + result.getProcessingTime());
            appResult.setBizProcessingTime(appResult.getBizProcessingTime() + result.getBizProcessingTime());
            appResult.setAccessCount(appResult.getAccessCount() + result.getAccessCount());
            appResult.setSlowExecutionCount(appResult.getSlowExecutionCount() + result.getSlowExecutionCount());
            appResult.setBizExceptionCount(appResult.getBizExceptionCount() + result.getBizExceptionCount());
            appResult.setOtherExceptionCount(appResult.getOtherExceptionCount() + result.getOtherExceptionCount());
        }
        return totalResults;
    }
}

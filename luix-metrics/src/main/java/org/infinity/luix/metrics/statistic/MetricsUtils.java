package org.infinity.luix.metrics.statistic;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.metrics.statistic.access.AccessResult;
import org.infinity.luix.metrics.statistic.access.AccessStatistics;
import org.infinity.luix.metrics.statistic.access.StatisticsType;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class MetricsUtils {
    /**
     * Access statistic interval in seconds
     */
    public static final  int                                     SCHEDULED_STATISTIC_INTERVAL = 30;
    public static        String                                  DELIMITER                    = "\\|";
    public static final  String                                  ELAPSED_TIME_HISTOGRAM       = MetricRegistry.name(AccessStatistics.class, "elapsedTime");
    private static final ConcurrentMap<String, AccessStatistics> ACCESS_STATISTICS            = new ConcurrentHashMap<>();

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
                                 long now, long processingTime, long bizProcessingTime,
                                 int slowExecutionThreshold, StatisticsType statisticsType) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        try {
            AccessStatistics item = getStatisticItem(name + "|" + application + "|" + module, now);
            item.log(now, processingTime, bizProcessingTime, slowExecutionThreshold, statisticsType);
        } catch (Exception e) {
            log.error("Failed to log access!", e);
        }
    }

    private static AccessStatistics getStatisticItem(String name, long now) {
        AccessStatistics item = ACCESS_STATISTICS.get(name);
        if (item == null) {
            ACCESS_STATISTICS.put(name, new AccessStatistics(name, now));
            item = ACCESS_STATISTICS.get(name);
        }
        return item;
    }

    public static ConcurrentMap<String, AccessResult> getTotalAccessStatistic() {
        ConcurrentMap<String, AccessResult> totalResults = new ConcurrentHashMap<>();
        for (Map.Entry<String, AccessStatistics> entry : ACCESS_STATISTICS.entrySet()) {
            AccessStatistics item = entry.getValue();
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

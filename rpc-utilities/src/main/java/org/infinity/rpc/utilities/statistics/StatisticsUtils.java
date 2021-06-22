package org.infinity.rpc.utilities.statistics;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.utilities.statistics.access.AccessStatisticItem;
import org.infinity.rpc.utilities.statistics.access.AccessStatisticResult;
import org.infinity.rpc.utilities.statistics.access.StatisticType;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public abstract class StatisticsUtils {
    /**
     * Access statistic interval in seconds
     */
    public static final  int                                        ACCESS_STATISTIC_INTERVAL = 30;
    public static        String                                     DELIMITER                 = "\\|";
    public static final  String                                     ELAPSED_TIME_HISTOGRAM    = MetricRegistry.name(AccessStatisticItem.class, "elapsedTimeMillis");
    private static final ConcurrentMap<String, AccessStatisticItem> ACCESS_STATISTICS         = new ConcurrentHashMap<>();

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

    public static void getAccessStatistic(String name,
                                          String application,
                                          String module,
                                          long currentTimeMillis,
                                          long costTimeMillis,
                                          long bizProcessTime,
                                          int slowThreshold,
                                          StatisticType statisticType) {
        if (StringUtils.isEmpty(name)) {
            return;
        }
        try {
            AccessStatisticItem item = getStatisticItem(name + "|" + application + "|" + module, currentTimeMillis);
            item.statistic(currentTimeMillis, costTimeMillis, bizProcessTime, slowThreshold, statisticType);
        } catch (Exception e) {
            log.error("Failed to calculate access statistic!", e);
        }
    }

    private static AccessStatisticItem getStatisticItem(String name, long currentTimeMillis) {
        AccessStatisticItem item = ACCESS_STATISTICS.get(name);
        if (item == null) {
            item = ACCESS_STATISTICS.putIfAbsent(name, new AccessStatisticItem(name, currentTimeMillis));
        }
        return item;
    }

    public static ConcurrentMap<String, AccessStatisticResult> getTotalAccessStatistic(int interval) {
        Validate.isTrue(interval <= ACCESS_STATISTIC_INTERVAL,
                "Interval must NOT be greater than " + ACCESS_STATISTIC_INTERVAL);

        ConcurrentMap<String, AccessStatisticResult> totalResults = new ConcurrentHashMap<>();
        for (Map.Entry<String, AccessStatisticItem> entry : ACCESS_STATISTICS.entrySet()) {
            AccessStatisticItem item = entry.getValue();
            AccessStatisticResult result = item.getStatisticResult(System.currentTimeMillis(), ACCESS_STATISTIC_INTERVAL);

            String key = entry.getKey();
            String[] keys = key.split(DELIMITER);
            if (keys.length != 3) {
                continue;
            }
            String application = keys[1];
            String module = keys[2];
            key = application + "|" + module;
            AccessStatisticResult appResult = totalResults.get(key);
            if (appResult == null) {
                totalResults.putIfAbsent(key, new AccessStatisticResult());
                appResult = totalResults.get(key);
            }
            appResult.totalCount += result.totalCount;
            appResult.bizExceptionCount += result.bizExceptionCount;
            appResult.slowCount += result.slowCount;
            appResult.costTime += result.costTime;
            appResult.bizTime += result.bizTime;
            appResult.otherExceptionCount += result.otherExceptionCount;
        }
        return totalResults;
    }
}

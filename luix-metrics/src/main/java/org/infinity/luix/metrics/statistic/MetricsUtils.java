package org.infinity.luix.metrics.statistic;

import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.metrics.statistic.access.ResponseType;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class MetricsUtils {

    private static final String LUIX_CALL_COUNT            = "luix_call_count";
    private static final String LUIX_SLOW_CALL_COUNT       = "luix_slow_call_count";
    private static final String LUIX_BIZ_EXCEPTION_COUNT   = "luix_biz_exception_count";
    private static final String LUIX_OTHER_EXCEPTION_COUNT = "luix_other_exception_count";
    private static final String LUIX_PROCESSING_TIME       = "luix_processing_time";
    private static final String LUIX_BIZ_PROCESSING_TIME   = "luix_biz_processing_time";
    private static final String METHOD                     = "method";

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

    public static void trackCall(String name, long processingTime, long bizProcessingTime,
                                 int slowThreshold, ResponseType responseType) {
        Validate.notNull(name, "name cannot be null");

        Metrics.timer(LUIX_PROCESSING_TIME, METHOD, name).record(processingTime, TimeUnit.MILLISECONDS);
        Metrics.timer(LUIX_BIZ_PROCESSING_TIME, METHOD, name).record(bizProcessingTime, TimeUnit.MILLISECONDS);
        Metrics.counter(LUIX_CALL_COUNT, METHOD, name).increment();
        if (processingTime >= slowThreshold) {
            Metrics.counter(LUIX_SLOW_CALL_COUNT, METHOD, name).increment();
        }
        if (responseType == ResponseType.BIZ_EXCEPTION) {
            Metrics.counter(LUIX_BIZ_EXCEPTION_COUNT, METHOD, name).increment();
        } else if (responseType == ResponseType.OTHER_EXCEPTION) {
            Metrics.counter(LUIX_OTHER_EXCEPTION_COUNT, METHOD, name).increment();
        }
    }
}

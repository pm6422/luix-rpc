package org.infinity.rpc.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;

@Slf4j
public abstract class StatisticsUtils {

    public static void logMemoryStatistic() {
        log.info("Memory usage: {} ", calculateMemory());
    }

    private static String calculateMemory() {
        Runtime runtime = Runtime.getRuntime();
        double totalMemory = (double) runtime.totalMemory() / (1024 * 1024);
        double freeMemory = (double) runtime.freeMemory() / (1024 * 1024);
        double maxMemory = (double) runtime.maxMemory() / (1024 * 1024);

        double usedMemory = totalMemory - freeMemory;
        double percentFree = ((maxMemory - usedMemory) / maxMemory) * 100.0;
        double percentUsed = 100 - percentFree;

        DecimalFormat storageFormat = new DecimalFormat("#0.00");
        DecimalFormat percentageFormat = new DecimalFormat("#0.0");

        return storageFormat.format(usedMemory) + "MB of " + storageFormat.format(maxMemory) + " MB (" +
                percentageFormat.format(percentUsed) + "%) used";
    }
}

package org.infinity.luix.utilities.statistics.access;


import com.codahale.metrics.Histogram;
import org.infinity.luix.utilities.statistics.CachedMetricsFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.infinity.luix.utilities.statistics.StatisticsUtils.ACCESS_STATISTIC_INTERVAL;
import static org.infinity.luix.utilities.statistics.StatisticsUtils.ELAPSED_TIME_HISTOGRAM;

public class AccessStatisticItem {
    private final String          name;
    private final AtomicInteger[] costTimes;
    private final AtomicInteger[] bizProcessTimes;
    private final AtomicInteger[] totalCounter;
    private final AtomicInteger[] slowCounter;
    private final AtomicInteger[] bizExceptionCounter;
    private final AtomicInteger[] otherExceptionCounter;
    private final Histogram       histogram;
    private final int             length;
    private       int             currentIndex;

    public AccessStatisticItem(String name, long currentTimeMillis) {
        this(name, currentTimeMillis, ACCESS_STATISTIC_INTERVAL * 2);
    }

    public AccessStatisticItem(String name, long currentTimeMillis, int length) {
        this.name = name;
        this.costTimes = initAtomicIntegerArray(length);
        this.bizProcessTimes = initAtomicIntegerArray(length);
        this.totalCounter = initAtomicIntegerArray(length);
        this.slowCounter = initAtomicIntegerArray(length);
        this.bizExceptionCounter = initAtomicIntegerArray(length);
        this.otherExceptionCounter = initAtomicIntegerArray(length);
        this.length = length;
        this.currentIndex = getIndex(currentTimeMillis, length);
        this.histogram = CachedMetricsFactory.getRegistryInstance(name).histogram(ELAPSED_TIME_HISTOGRAM);
    }

    private AtomicInteger[] initAtomicIntegerArray(int size) {
        AtomicInteger[] values = new AtomicInteger[size];
        for (int i = 0; i < values.length; i++) {
            values[i] = new AtomicInteger(0);
        }
        return values;
    }

    /**
     * @param currentTimeMillis current time in milliseconds
     * @param elapsedTimeMillis elapsed time in milliseconds
     * @param bizProcessTime    business process time
     * @param statisticType     statistic type
     */
    public void statistic(long currentTimeMillis, long elapsedTimeMillis, long bizProcessTime, int slowCost, StatisticType statisticType) {
        int tempIndex = getIndex(currentTimeMillis, length);

        if (currentIndex != tempIndex) {
            synchronized (this) {
                // 这一秒的第一条统计，把对应的存储位的数据置0
                if (currentIndex != tempIndex) {
                    reset(tempIndex);
                    currentIndex = tempIndex;
                }
            }
        }

        costTimes[currentIndex].addAndGet((int) elapsedTimeMillis);
        bizProcessTimes[currentIndex].addAndGet((int) bizProcessTime);
        totalCounter[currentIndex].incrementAndGet();

        if (elapsedTimeMillis >= slowCost) {
            slowCounter[currentIndex].incrementAndGet();
        }

        if (statisticType == StatisticType.BIZ_EXCEPTION) {
            bizExceptionCounter[currentIndex].incrementAndGet();
        } else if (statisticType == StatisticType.OTHER_EXCEPTION) {
            otherExceptionCounter[currentIndex].incrementAndGet();
        }
        histogram.update(elapsedTimeMillis);
        String[] names = name.split("\\|");
        String appName = names[1] + "|" + names[2];
        CachedMetricsFactory.getRegistryInstance(appName).histogram(ELAPSED_TIME_HISTOGRAM)
                .update(elapsedTimeMillis);
    }

    private int getIndex(long currentTimeMillis, int periodSecond) {
        return (int) ((currentTimeMillis / 1000) % periodSecond);
    }

    private void reset(int index) {
        costTimes[index].set(0);
        totalCounter[index].set(0);
        bizProcessTimes[index].set(0);
        slowCounter[index].set(0);
        bizExceptionCounter[index].set(0);
        otherExceptionCounter[index].set(0);
    }

    public AccessStatisticResult getStatisticResult(long currentTimeMillis, int interval) {
        long currentTimeSecond = currentTimeMillis / 1000;
        currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移interval
        int startIndex = getIndex(currentTimeSecond * 1000, length);
        AccessStatisticResult result = new AccessStatisticResult();

        for (int i = 0; i < interval; i++) {
            int currentIndex = (startIndex - i + length) % length;

            result.costTime += costTimes[currentIndex].get();
            result.bizTime += bizProcessTimes[currentIndex].get();
            result.totalCount += totalCounter[currentIndex].get();
            result.slowCount += slowCounter[currentIndex].get();
            result.bizExceptionCount += bizExceptionCounter[currentIndex].get();
            result.otherExceptionCount += otherExceptionCounter[currentIndex].get();
            if (totalCounter[currentIndex].get() > result.maxCount) {
                result.maxCount = totalCounter[currentIndex].get();
            } else if (totalCounter[currentIndex].get() < result.minCount || result.minCount == -1) {
                result.minCount = totalCounter[currentIndex].get();
            }
        }
        return result;
    }

    public void clearStatistic(long currentTimeMillis, int interval) {
        long currentTimeSecond = currentTimeMillis / 1000;
        currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移interval

        int startIndex = getIndex(currentTimeSecond * 1000, length);
        for (int i = 0; i < interval; i++) {
            int currentIndex = (startIndex - i + length) % length;
            reset(currentIndex);
        }
    }
}
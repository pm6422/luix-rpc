package org.infinity.rpc.utilities.statistics.access;


import com.codahale.metrics.Histogram;

import java.util.concurrent.atomic.AtomicInteger;

import static org.infinity.rpc.utilities.statistics.StatisticsUtils.ACCESS_STATISTIC_INTERVAL;
import static org.infinity.rpc.utilities.statistics.StatisticsUtils.HISTOGRAM_NAME;

public class AccessStatisticItem {
    private String          name;
    private int             currentIndex;
    private AtomicInteger[] costTimes;
    private AtomicInteger[] bizProcessTimes;
    private AtomicInteger[] totalCounter;
    private AtomicInteger[] slowCounter;
    private AtomicInteger[] bizExceptionCounter;
    private AtomicInteger[] otherExceptionCounter;
    private Histogram       histogram;
    private int             length;

    public AccessStatisticItem(String name, long currentTimeMillis) {
        this(name, currentTimeMillis, ACCESS_STATISTIC_INTERVAL * 2);
    }

    public AccessStatisticItem(String name, long currentTimeMillis, int length) {
        this.name = name;
        this.costTimes = initAtomicIntegerArr(length);
        this.bizProcessTimes = initAtomicIntegerArr(length);
        this.totalCounter = initAtomicIntegerArr(length);
        this.slowCounter = initAtomicIntegerArr(length);
        this.bizExceptionCounter = initAtomicIntegerArr(length);
        this.otherExceptionCounter = initAtomicIntegerArr(length);
        this.length = length;
        this.currentIndex = getIndex(currentTimeMillis, length);
        this.histogram = InternalMetricsFactory.getRegistryInstance(name).histogram(HISTOGRAM_NAME);
    }

    private AtomicInteger[] initAtomicIntegerArr(int size) {
        AtomicInteger[] arrs = new AtomicInteger[size];
        for (int i = 0; i < arrs.length; i++) {
            arrs[i] = new AtomicInteger(0);
        }

        return arrs;
    }

    /**
     * currentTimeMillis: 此刻记录的时间 (ms) costTimeMillis: 这次操作的耗时 (ms)
     *
     * @param currentTimeMillis
     * @param costTimeMillis
     * @param bizProcessTime
     * @param accessStatus
     */
    public void statistic(long currentTimeMillis, long costTimeMillis, long bizProcessTime, int slowCost, AccessStatus accessStatus) {
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

        costTimes[currentIndex].addAndGet((int) costTimeMillis);
        bizProcessTimes[currentIndex].addAndGet((int) bizProcessTime);
        totalCounter[currentIndex].incrementAndGet();

        if (costTimeMillis >= slowCost) {
            slowCounter[currentIndex].incrementAndGet();
        }

        if (accessStatus == AccessStatus.BIZ_EXCEPTION) {
            bizExceptionCounter[currentIndex].incrementAndGet();
        } else if (accessStatus == AccessStatus.OTHER_EXCEPTION) {
            otherExceptionCounter[currentIndex].incrementAndGet();
        }
        histogram.update(costTimeMillis);
        String[] names = name.split("\\|");
        String appName = names[1] + "|" + names[2];
        InternalMetricsFactory.getRegistryInstance(appName).histogram(HISTOGRAM_NAME)
                .update(costTimeMillis);
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

    public AccessStatisticResult getStatisticResult(long currentTimeMillis, int peroidSecond) {
        long currentTimeSecond = currentTimeMillis / 1000;
        currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移peroidSecond

        int startIndex = getIndex(currentTimeSecond * 1000, length);

        AccessStatisticResult result = new AccessStatisticResult();

        for (int i = 0; i < peroidSecond; i++) {
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

    void clearStatistic(long currentTimeMillis, int peroidSecond) {
        long currentTimeSecond = currentTimeMillis / 1000;
        currentTimeSecond--; // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移peroidSecond

        int startIndex = getIndex(currentTimeSecond * 1000, length);

        for (int i = 0; i < peroidSecond; i++) {
            int currentIndex = (startIndex - i + length) % length;

            reset(currentIndex);
        }
    }

}
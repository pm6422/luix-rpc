package org.infinity.luix.metrics.statistic.access;

import com.codahale.metrics.Histogram;
import org.infinity.luix.metrics.statistic.CachedMetricsFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.infinity.luix.metrics.statistic.MetricsUtils.ELAPSED_TIME_HISTOGRAM;
import static org.infinity.luix.metrics.statistic.MetricsUtils.SCHEDULED_STATISTIC_INTERVAL;

public class AccessStatistics {
    private static final int          INTERVAL_SECONDS = SCHEDULED_STATISTIC_INTERVAL * 2;
    private final        String       name;
    private final        AtomicLong[] accessCounter;
    private final        AtomicLong[] slowExecutionCounter;
    private final        AtomicLong[] processingTimes;
    private final        AtomicLong[] bizProcessingTimes;
    private final        AtomicLong[] bizExceptionCounter;
    private final        AtomicLong[] otherExceptionCounter;
    private volatile     int          currentIndex;
    private final        Histogram    histogram;

    public AccessStatistics(String name, long now) {
        this.name = name;
        this.accessCounter = initAtomicIntegerArray();
        this.slowExecutionCounter = initAtomicIntegerArray();
        this.processingTimes = initAtomicIntegerArray();
        this.bizProcessingTimes = initAtomicIntegerArray();
        this.bizExceptionCounter = initAtomicIntegerArray();
        this.otherExceptionCounter = initAtomicIntegerArray();

        this.currentIndex = getIndex(now);
        this.histogram = CachedMetricsFactory.getMetricsRegistry(name).histogram(ELAPSED_TIME_HISTOGRAM);
    }

    private AtomicLong[] initAtomicIntegerArray() {
        return IntStream.range(0, INTERVAL_SECONDS)
                .mapToObj(i -> new AtomicLong(0L))
                .toArray(AtomicLong[]::new);
    }

    private int getIndex(long now) {
        return (int) ((now / 1_000) % INTERVAL_SECONDS);
    }

    public void log(long now, long elapsedTime, long bizProcessingTime,
                    int slowExecutionThreshold, StatisticsType statisticsType) {
        int index = getIndex(now);
        if (currentIndex != index) {
            synchronized (this) {
                if (currentIndex != index) {
                    // 这一秒的第一条统计，把对应的存储位的数据置0
                    reset(index);
                    currentIndex = index;
                }
            }
        }

        accessCounter[currentIndex].incrementAndGet();
        if (elapsedTime >= slowExecutionThreshold) {
            slowExecutionCounter[currentIndex].incrementAndGet();
        }
        processingTimes[currentIndex].addAndGet(elapsedTime);
        bizProcessingTimes[currentIndex].addAndGet(bizProcessingTime);
        if (statisticsType == StatisticsType.BIZ_EXCEPTION) {
            bizExceptionCounter[currentIndex].incrementAndGet();
        } else if (statisticsType == StatisticsType.OTHER_EXCEPTION) {
            otherExceptionCounter[currentIndex].incrementAndGet();
        }
        histogram.update(elapsedTime);
        String[] names = name.split("\\|");
        String appName = names[1] + "|" + names[2];
        CachedMetricsFactory.getMetricsRegistry(appName).histogram(ELAPSED_TIME_HISTOGRAM).update(elapsedTime);
    }

    private void reset(int index) {
        accessCounter[index].set(0);
        slowExecutionCounter[index].set(0);
        processingTimes[index].set(0);
        bizProcessingTimes[index].set(0);
        bizExceptionCounter[index].set(0);
        otherExceptionCounter[index].set(0);
    }

    public void clear(long now, int interval) {
        // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移interval
        int startIndex = getIndex(Instant.ofEpochMilli(now).minusSeconds(1).toEpochMilli());
        for (int i = 0; i < interval; i++) {
            int currentIndex = (startIndex - i + INTERVAL_SECONDS) % INTERVAL_SECONDS;
            reset(currentIndex);
        }
    }

    public AccessResult getStatisticResult(long now, int interval) {
        // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移interval
        int startIndex = getIndex(Instant.ofEpochMilli(now).minusSeconds(1).toEpochMilli());

        AccessResult result = new AccessResult();
        for (int i = 0; i < interval; i++) {
            int currentIndex = (startIndex - i + INTERVAL_SECONDS) % INTERVAL_SECONDS;

            result.setAccessCount(result.getAccessCount() + accessCounter[currentIndex].get());
            result.setSlowExecutionCount(result.getSlowExecutionCount() + slowExecutionCounter[currentIndex].get());
            result.setProcessingTime(result.getProcessingTime() + processingTimes[currentIndex].get());
            result.setBizProcessingTime(result.getBizProcessingTime() + bizProcessingTimes[currentIndex].get());
            result.setBizExceptionCount(result.getBizExceptionCount() + bizExceptionCounter[currentIndex].get());
            result.setOtherExceptionCount(result.getOtherExceptionCount() + otherExceptionCounter[currentIndex].get());

            if (accessCounter[currentIndex].get() > result.getMaxCount()) {
                result.setMaxCount(accessCounter[currentIndex].get());
            } else if (accessCounter[currentIndex].get() < result.getMinCount() || result.getMinCount() == -1) {
                result.setMinCount(accessCounter[currentIndex].get());
            }
        }
        return result;
    }
}
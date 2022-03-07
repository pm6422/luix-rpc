package org.infinity.luix.metrics.statistic.access;

import com.codahale.metrics.Histogram;
import org.infinity.luix.metrics.statistic.CachedMetricsFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.infinity.luix.metrics.statistic.MetricsUtils.*;

public class Metric {
    private static final int          INTERVAL_SECONDS = SCHEDULED_STATISTIC_INTERVAL * 2;
    private final        String       name;
    private final        AtomicLong[] callCounters;
    private final        AtomicLong[] slowExecutionCounters;
    private final        AtomicLong[] processingTimeCounters;
    private final        AtomicLong[] bizProcessingTimeCounters;
    private final        AtomicLong[] bizExceptionCounters;
    private final        AtomicLong[] otherExceptionCounters;
    private volatile     int          currentIndex;
    private final        Histogram    histogram;

    public Metric(String name, long timestamp) {
        this.name = name;
        this.callCounters = initAtomicLongArray();
        this.slowExecutionCounters = initAtomicLongArray();
        this.processingTimeCounters = initAtomicLongArray();
        this.bizProcessingTimeCounters = initAtomicLongArray();
        this.bizExceptionCounters = initAtomicLongArray();
        this.otherExceptionCounters = initAtomicLongArray();

        this.currentIndex = getIndex(timestamp);
        this.histogram = CachedMetricsFactory.getMetricsRegistry(name).histogram(PROCESSING_TIME_HISTOGRAM);
    }

    private AtomicLong[] initAtomicLongArray() {
        return IntStream.range(0, INTERVAL_SECONDS)
                .mapToObj(i -> new AtomicLong(0L))
                .toArray(AtomicLong[]::new);
    }

    private int getIndex(long timestamp) {
        return (int) ((timestamp / 1_000) % INTERVAL_SECONDS);
    }

    public void track(long timestamp, long processingTime, long bizProcessingTime, int slowThreshold, ResponseType responseType) {
        int index = getIndex(timestamp);
        if (currentIndex != index) {
            synchronized (this) {
                if (currentIndex != index) {
                    // 这一秒的第一条统计，把对应的存储位的数据置0
                    reset(index);
                    currentIndex = index;
                }
            }
        }

        callCounters[currentIndex].incrementAndGet();
        if (processingTime >= slowThreshold) {
            slowExecutionCounters[currentIndex].incrementAndGet();
        }
        processingTimeCounters[currentIndex].addAndGet(processingTime);
        bizProcessingTimeCounters[currentIndex].addAndGet(bizProcessingTime);
        if (responseType == ResponseType.BIZ_EXCEPTION) {
            bizExceptionCounters[currentIndex].incrementAndGet();
        } else if (responseType == ResponseType.OTHER_EXCEPTION) {
            otherExceptionCounters[currentIndex].incrementAndGet();
        }

        histogram.update(processingTime);

        CachedMetricsFactory
                .getMetricsRegistry(PROCESSING_TIME_METRICS_REGISTRY)
                .histogram(PROCESSING_TIME_HISTOGRAM)
                .update(processingTime);
    }

    private void reset(int index) {
        callCounters[index].set(0);
        slowExecutionCounters[index].set(0);
        processingTimeCounters[index].set(0);
        bizProcessingTimeCounters[index].set(0);
        bizExceptionCounters[index].set(0);
        otherExceptionCounters[index].set(0);
    }

    public void clear(long now, int interval) {
        // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移interval
        int startIndex = getIndex(Instant.ofEpochMilli(now).minusSeconds(1).toEpochMilli());
        for (int i = 0; i < interval; i++) {
            int currentIndex = (startIndex - i + INTERVAL_SECONDS) % INTERVAL_SECONDS;
            reset(currentIndex);
        }
    }

    public CallMetric getCallMetric(long timestamp, int interval) {
        // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移interval
        int startIndex = getIndex(Instant.ofEpochMilli(timestamp).minusSeconds(1).toEpochMilli());

        CallMetric result = new CallMetric();
        for (int i = 0; i < interval; i++) {
            int currentIndex = (startIndex - i + INTERVAL_SECONDS) % INTERVAL_SECONDS;

            result.setAccessCount(result.getAccessCount() + callCounters[currentIndex].get());
            result.setSlowExecutionCount(result.getSlowExecutionCount() + slowExecutionCounters[currentIndex].get());
            result.setProcessingTime(result.getProcessingTime() + processingTimeCounters[currentIndex].get());
            result.setBizProcessingTime(result.getBizProcessingTime() + bizProcessingTimeCounters[currentIndex].get());
            result.setBizExceptionCount(result.getBizExceptionCount() + bizExceptionCounters[currentIndex].get());
            result.setOtherExceptionCount(result.getOtherExceptionCount() + otherExceptionCounters[currentIndex].get());

            if (callCounters[currentIndex].get() > result.getMaxCount()) {
                result.setMaxCount(callCounters[currentIndex].get());
            } else if (callCounters[currentIndex].get() < result.getMinCount() || result.getMinCount() == -1) {
                result.setMinCount(callCounters[currentIndex].get());
            }
        }
        return result;
    }
}
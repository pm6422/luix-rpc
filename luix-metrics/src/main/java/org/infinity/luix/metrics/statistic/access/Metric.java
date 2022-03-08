package org.infinity.luix.metrics.statistic.access;

import com.codahale.metrics.MetricRegistry;
import org.infinity.luix.metrics.statistic.CachedMetricsFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.infinity.luix.metrics.statistic.MetricsUtils.SCHEDULED_STATISTIC_INTERVAL;

public class Metric {
    private static final int    INTERVAL_SECONDS                 = SCHEDULED_STATISTIC_INTERVAL * 2;
    public static final  String PROCESSING_TIME_METRICS_REGISTRY = "defaultProcessingTime";
    public static final  String PROCESSING_TIME_HISTOGRAM        = MetricRegistry.name(Metric.class, "processingTime");

    private final    String       name;
    private final    AtomicLong[] processingTimers;
    private final    AtomicLong[] bizProcessingTimers;
    private final    AtomicLong[] callCounters;
    private final    AtomicLong[] slowExecutionCounters;
    private final    AtomicLong[] bizExceptionCounters;
    private final    AtomicLong[] otherExceptionCounters;
    private volatile int          currentIndex;

    public Metric(String name, long timestamp) {
        this.name = name;
        this.processingTimers = initAtomicLongArray();
        this.bizProcessingTimers = initAtomicLongArray();
        this.callCounters = initAtomicLongArray();
        this.slowExecutionCounters = initAtomicLongArray();
        this.bizExceptionCounters = initAtomicLongArray();
        this.otherExceptionCounters = initAtomicLongArray();

        this.currentIndex = getIndex(timestamp);
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

        processingTimers[currentIndex].addAndGet(processingTime);
        bizProcessingTimers[currentIndex].addAndGet(bizProcessingTime);

        callCounters[currentIndex].incrementAndGet();
        if (processingTime >= slowThreshold) {
            slowExecutionCounters[currentIndex].incrementAndGet();
        }
        if (responseType == ResponseType.BIZ_EXCEPTION) {
            bizExceptionCounters[currentIndex].incrementAndGet();
        } else if (responseType == ResponseType.OTHER_EXCEPTION) {
            otherExceptionCounters[currentIndex].incrementAndGet();
        }

        // Add to processing time histogram
        CachedMetricsFactory
                .getMetricsRegistry(PROCESSING_TIME_METRICS_REGISTRY)
                .histogram(PROCESSING_TIME_HISTOGRAM)
                .update(processingTime);
    }

    private void reset(int index) {
        processingTimers[index].set(0);
        bizProcessingTimers[index].set(0);
        callCounters[index].set(0);
        slowExecutionCounters[index].set(0);
        bizExceptionCounters[index].set(0);
        otherExceptionCounters[index].set(0);
    }

    public void clear(long timestamp, int interval) {
        // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移interval
        int startIndex = getIndex(Instant.ofEpochMilli(timestamp).minusSeconds(1).toEpochMilli());
        for (int i = 0; i < interval; i++) {
            int currentIndex = (startIndex - i + INTERVAL_SECONDS) % INTERVAL_SECONDS;
            reset(currentIndex);
        }
    }

    public CallMetric getCallMetric(long timestamp, int interval) {
        // 当前这秒还没完全结束，因此数据不全，统计从上一秒开始，往前推移interval
        int startIndex = getIndex(Instant.ofEpochMilli(timestamp).minusSeconds(1).toEpochMilli());

        CallMetric callMetric = new CallMetric();
        for (int i = 0; i < interval; i++) {
            int currentIndex = (startIndex - i + INTERVAL_SECONDS) % INTERVAL_SECONDS;

            callMetric.setProcessingTime(callMetric.getProcessingTime() + processingTimers[currentIndex].get());
            callMetric.setBizProcessingTime(callMetric.getBizProcessingTime() + bizProcessingTimers[currentIndex].get());
            callMetric.setCallCount(callMetric.getCallCount() + callCounters[currentIndex].get());
            callMetric.setSlowExecutionCount(callMetric.getSlowExecutionCount() + slowExecutionCounters[currentIndex].get());
            callMetric.setBizExceptionCount(callMetric.getBizExceptionCount() + bizExceptionCounters[currentIndex].get());
            callMetric.setOtherExceptionCount(callMetric.getOtherExceptionCount() + otherExceptionCounters[currentIndex].get());

            if (callCounters[currentIndex].get() > callMetric.getMaxCallCount()) {
                callMetric.setMaxCallCount(callCounters[currentIndex].get());
            } else if (callCounters[currentIndex].get() < callMetric.getMinCallCount() || callMetric.getMinCallCount() == -1) {
                callMetric.setMinCallCount(callCounters[currentIndex].get());
            }
        }
        return callMetric;
    }
}
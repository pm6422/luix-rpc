package org.infinity.luix.metrics.statistic.access;

import lombok.Data;

@Data
public class CallMetric {
    private double processingTime      = 0;
    private double bizProcessingTime   = 0;
    private long   accessCount         = 0;
    private long   slowExecutionCount  = 0;
    private long   bizExceptionCount   = 0;
    private long   otherExceptionCount = 0;

    private long   maxCount            = -1;
    private long   minCount            = -1;

    public void increment(CallMetric currentCallMetric) {
        processingTime += currentCallMetric.getProcessingTime();
        bizProcessingTime += currentCallMetric.getBizProcessingTime();
        accessCount += currentCallMetric.getAccessCount();
        slowExecutionCount += currentCallMetric.getSlowExecutionCount();
        bizExceptionCount += currentCallMetric.getBizExceptionCount();
        otherExceptionCount += currentCallMetric.getOtherExceptionCount();
    }
}
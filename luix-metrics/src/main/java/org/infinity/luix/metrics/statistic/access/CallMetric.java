package org.infinity.luix.metrics.statistic.access;

import lombok.Data;

@Data
public class CallMetric {
    private double processingTime      = 0;
    private double bizProcessingTime   = 0;
    private long   accessCount         = 0;
    private long   maxCount            = -1;
    private long   minCount            = -1;
    private long   slowExecutionCount  = 0;
    private long   bizExceptionCount   = 0;
    private long   otherExceptionCount = 0;
}
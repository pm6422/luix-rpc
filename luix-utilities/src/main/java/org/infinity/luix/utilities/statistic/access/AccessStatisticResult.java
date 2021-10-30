package org.infinity.luix.utilities.statistic.access;

public class AccessStatisticResult {
    public int    totalCount          = 0;
    public int    maxCount            = -1;
    public int    minCount            = -1;
    public int    slowCount           = 0;
    public int    bizExceptionCount   = 0;
    public int    otherExceptionCount = 0;
    public double costTime            = 0;
    public double bizTime             = 0;
    public long   slowThreshold       = 200;
    public long[] intervalCounts      = new long[5];
}
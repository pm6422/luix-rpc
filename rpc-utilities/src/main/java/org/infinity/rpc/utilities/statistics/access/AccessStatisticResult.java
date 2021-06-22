package org.infinity.rpc.utilities.statistics.access;

import com.codahale.metrics.Histogram;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-26
 * 
 */
public class AccessStatisticResult {
    public int totalCount = 0;
    public int maxCount = -1;
    public int minCount = -1;

    public int slowCount = 0;
    public int bizExceptionCount = 0;
    public int otherExceptionCount = 0;

    public Histogram histogram = null;

    public double costTime = 0;
    public double bizTime = 0;

    public long slowThreshold = 200;
    public long[] intervalCounts = new long[5];

}
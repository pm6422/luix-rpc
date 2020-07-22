package org.infinity.rpc.utilities.id;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.infinity.rpc.utilities.id.sequence.SnowFlakeSequence;
import org.junit.Rule;
import org.junit.Test;

/**
 * 性能测试
 */
public class PerformanceTest {
    @Rule
    public  ContiPerfRule     i                 = new ContiPerfRule();
    private SnowFlakeSequence snowFlakeSequence = new SnowFlakeSequence(0);

    @Test
    @PerfTest(invocations = 200000, threads = 16)
    public void test1() {
        snowFlakeSequence.nextId();
    }
}
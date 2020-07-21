package org.infinity.rpc.utilities.id;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * 性能测试
 */
public class PerformanceTest {
    @Rule
    public  ContiPerfRule i        = new ContiPerfRule();
    private Sequence      sequence = new Sequence(0);

    @Test
    @PerfTest(invocations = 200000, threads = 16)
    public void test1() {
        sequence.nextId();
    }
}
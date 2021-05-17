package org.infinity.rpc.utilities.id;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;

public class PerfTests {
    @Rule
    public  ContiPerfRule     i                 = new ContiPerfRule();

    @Test
    @PerfTest(invocations = 200000, threads = 16)
    public void testSnowFlakeId() {
        IdGenerator.generateSnowFlakeId();
    }

    @Test
    @PerfTest(invocations = 200000, threads = 16)
    public void testTimestampId() {
        IdGenerator.generateTimestampId();
    }

    @Test
    @PerfTest(invocations = 200, threads = 16)
    public void testShortId() {
        IdGenerator.generateShortId();
    }
}
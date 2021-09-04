package org.infinity.luix.utilities.id;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.infinity.luix.utilities.id.IdGenerator;
import org.infinity.luix.utilities.id.SnowFlakeIdGenerator;
import org.junit.Rule;
import org.junit.Test;

public class PerfTests {
    @Rule
    public               ContiPerfRule        i                       = new ContiPerfRule();
    private static final SnowFlakeIdGenerator SNOW_FLAKE_ID_GENERATOR = new SnowFlakeIdGenerator(1L, false, false);

    @Test
    @PerfTest(invocations = 200000, threads = 16)
    public void testSnowFlakeId() {
        SNOW_FLAKE_ID_GENERATOR.nextId();
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
package org.infinity.rpc.utilities.serializer.kryo;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.infinity.rpc.utilities.serializer.Serializer;
import org.infinity.rpc.utilities.serviceloader.ServiceLoader;
import org.junit.Rule;
import org.junit.Test;

import static org.infinity.rpc.utilities.serializer.Serializer.SERIALIZER_NAME_KRYO;

public class KryoSerializerPerfTests {
    @Rule
    public ContiPerfRule i = new ContiPerfRule();

    @Test
    @PerfTest(invocations = 10000, threads = 16, rampUp = 100, warmUp = 10)
    public void kryoPerf() {
        Serializer serializer = ServiceLoader.forClass(Serializer.class).load(SERIALIZER_NAME_KRYO);

    }
}

package org.infinity.rpc.utilities.spi.testservice;

import org.infinity.rpc.utilities.spi.annotation.ServiceName;
import java.util.concurrent.atomic.AtomicLong;

@ServiceName("spitest")
public class SpiSingletonImpl implements SpiSingletonInterface {
    private static AtomicLong counter = new AtomicLong(0);
    private        long       index   = 0;

    public SpiSingletonImpl() {
        index = counter.incrementAndGet();
    }

    @Override
    public long spiHello() {
        return index;
    }

}

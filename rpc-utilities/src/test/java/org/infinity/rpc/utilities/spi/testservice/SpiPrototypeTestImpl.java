package org.infinity.rpc.utilities.spi.testservice;


import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.concurrent.atomic.AtomicLong;

@ServiceName("spiPrototypeTest")
public class SpiPrototypeTestImpl implements SpiPrototypeInterface {
    private static AtomicLong counter = new AtomicLong(0);
    private        long       index   = 0;

    public SpiPrototypeTestImpl() {
        index = counter.incrementAndGet();
    }

    @Override
    public long spiHello() {
        return index;
    }

}

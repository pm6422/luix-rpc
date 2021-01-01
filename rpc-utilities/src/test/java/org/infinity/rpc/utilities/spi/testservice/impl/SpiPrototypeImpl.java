package org.infinity.rpc.utilities.spi.testservice.impl;


import org.infinity.rpc.utilities.spi.annotation.ServiceName;
import org.infinity.rpc.utilities.spi.testservice.SpiPrototypeInterface;

import java.util.concurrent.atomic.AtomicLong;

@ServiceName("prototype")
public class SpiPrototypeImpl implements SpiPrototypeInterface {
    private static AtomicLong counter = new AtomicLong(0);
    private        long       index   = 0;

    public SpiPrototypeImpl() {
        index = counter.incrementAndGet();
    }

    @Override
    public long spiHello() {
        return index;
    }

}

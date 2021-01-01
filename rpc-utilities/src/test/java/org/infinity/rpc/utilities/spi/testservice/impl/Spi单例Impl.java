package org.infinity.rpc.utilities.spi.testservice.impl;

import org.infinity.rpc.utilities.spi.annotation.ServiceName;
import org.infinity.rpc.utilities.spi.testservice.SpiSingletonInterface;

import java.util.concurrent.atomic.AtomicLong;

@ServiceName("单例")
public class Spi单例Impl implements SpiSingletonInterface {
    private static AtomicLong counter = new AtomicLong(0);
    private        long       index   = 0;

    public Spi单例Impl() {
        index = counter.incrementAndGet();
    }

    @Override
    public long spiHello() {
        return index;
    }

}

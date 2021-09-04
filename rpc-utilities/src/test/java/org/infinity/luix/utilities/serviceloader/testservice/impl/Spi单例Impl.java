package org.infinity.luix.utilities.serviceloader.testservice.impl;

import org.infinity.luix.utilities.serviceloader.annotation.SpiName;
import org.infinity.luix.utilities.serviceloader.testservice.SpiSingletonInterface;

import java.util.concurrent.atomic.AtomicLong;

@SpiName("单例")
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

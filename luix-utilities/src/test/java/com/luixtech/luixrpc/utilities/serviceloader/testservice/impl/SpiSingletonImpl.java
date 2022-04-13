package com.luixtech.luixrpc.utilities.serviceloader.testservice.impl;

import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiName;
import com.luixtech.luixrpc.utilities.serviceloader.testservice.SpiSingletonInterface;

import java.util.concurrent.atomic.AtomicLong;

@SpiName("singleton")
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

package com.luixtech.luixrpc.utilities.serviceloader.testservice.impl;


import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiName;
import com.luixtech.luixrpc.utilities.serviceloader.testservice.SpiPrototypeInterface;

import java.util.concurrent.atomic.AtomicLong;

@SpiName("prototype")
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

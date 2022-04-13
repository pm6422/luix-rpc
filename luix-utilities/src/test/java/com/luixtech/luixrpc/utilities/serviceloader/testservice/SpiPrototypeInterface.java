package com.luixtech.luixrpc.utilities.serviceloader.testservice;

import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiScope;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.Spi;

@Spi(scope = SpiScope.PROTOTYPE)
public interface SpiPrototypeInterface {
    long spiHello();
}

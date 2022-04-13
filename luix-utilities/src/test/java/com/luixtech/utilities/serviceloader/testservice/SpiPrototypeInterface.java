package com.luixtech.utilities.serviceloader.testservice;

import com.luixtech.utilities.serviceloader.annotation.SpiScope;
import com.luixtech.utilities.serviceloader.annotation.Spi;

@Spi(scope = SpiScope.PROTOTYPE)
public interface SpiPrototypeInterface {
    long spiHello();
}

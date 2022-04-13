package com.luixtech.utilities.serviceloader.testservice;


import com.luixtech.utilities.serviceloader.annotation.SpiScope;
import com.luixtech.utilities.serviceloader.annotation.Spi;

@Spi(scope = SpiScope.SINGLETON)
public interface SpiSingletonInterface {
    long spiHello();
}
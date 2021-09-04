package org.infinity.luix.utilities.serviceloader.testservice;


import org.infinity.luix.utilities.serviceloader.annotation.SpiScope;
import org.infinity.luix.utilities.serviceloader.annotation.Spi;

@Spi(scope = SpiScope.SINGLETON)
public interface SpiSingletonInterface {
    long spiHello();
}
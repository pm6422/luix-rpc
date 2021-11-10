package org.infinity.luix.utilities.serviceloader.testservice;

import org.infinity.luix.utilities.serviceloader.annotation.SpiScope;
import org.infinity.luix.utilities.serviceloader.annotation.Spi;

@Spi(scope = SpiScope.PROTOTYPE)
public interface SpiPrototypeInterface {
    long spiHello();
}

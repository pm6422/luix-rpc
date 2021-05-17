package org.infinity.rpc.utilities.serviceloader.testservice;

import org.infinity.rpc.utilities.serviceloader.annotation.SpiScope;
import org.infinity.rpc.utilities.serviceloader.annotation.Spi;

@Spi(scope = SpiScope.PROTOTYPE)
public interface SpiPrototypeInterface {
    long spiHello();
}

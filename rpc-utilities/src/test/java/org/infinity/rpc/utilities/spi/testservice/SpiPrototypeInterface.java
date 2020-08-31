package org.infinity.rpc.utilities.spi.testservice;

import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface SpiPrototypeInterface {
    long spiHello();
}

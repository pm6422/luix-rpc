package org.infinity.rpc.utilities.spi.testservice;

import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = Scope.PROTOTYPE)
public interface SpiPrototypeInterface {
    long spiHello();
}

package org.infinity.rpc.core.spi.testservice;

import org.infinity.rpc.core.spi.annotation.Scope;
import org.infinity.rpc.core.spi.annotation.Spi;

@Spi(scope = Scope.SINGLETON)
public interface SpiSingletonInterface {
    long spiHello();
}
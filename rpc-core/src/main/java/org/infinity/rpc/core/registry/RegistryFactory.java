package org.infinity.rpc.core.registry;

import org.infinity.rpc.core.spi.annotation.Scope;
import org.infinity.rpc.core.spi.annotation.Spi;

@Spi(scope = Scope.SINGLETON)
public interface RegistryFactory {
    Registry getRegistry(Url url);
}

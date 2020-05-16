package org.infinity.rpc.core.registry;


import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = Scope.SINGLETON)
public interface RegistryFactory {

    Registry getRegistry(Url registryUrl);

    Registry createRegistry(Url registryUrl);

}

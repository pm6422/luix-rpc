package org.infinity.rpc.core.registry;


import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = Scope.SINGLETON)
public interface RegistryFactory {

    Registry getRegistry(Url registryUrl);

    Registry createRegistry(Url registryUrl);

    /**
     * Get the registry factory based on protocol
     *
     * @param protocol protocol
     * @return registry factory
     */
    static RegistryFactory getInstance(String protocol) {
        // Get the property registry factory by protocol value
        RegistryFactory registryFactory = ServiceInstanceLoader.getServiceLoader(RegistryFactory.class).load(protocol);
        return registryFactory;
    }
}

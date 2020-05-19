package org.infinity.rpc.core.registry;


import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

/**
 * Registry factory used to create registry
 */
@Spi(scope = Scope.SINGLETON)
public interface RegistryFactory {

    /**
     * Get or create a registry
     *
     * @param registryUrl registry url
     * @return specified registry instance
     */
    Registry getRegistry(Url registryUrl);

    /**
     * Create a registry
     *
     * @param registryUrl registry URL
     * @return specified registry instance
     */
    Registry createRegistry(Url registryUrl);

    /**
     * Get the registry factory based on protocol
     *
     * @param protocol protocol
     * @return registry factory
     */
    static RegistryFactory getInstance(String protocol) {
        // Get the proper registry factory by protocol value
        RegistryFactory registryFactory = ServiceInstanceLoader.getServiceLoader(RegistryFactory.class).load(protocol);
        return registryFactory;
    }
}

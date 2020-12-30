package org.infinity.rpc.core.registry;


import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

/**
 * Registry factory used to create registry
 */
@Spi(scope = SpiScope.SINGLETON)
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
     * @param protocolName protocol name
     * @return registry factory
     */
    static RegistryFactory getInstance(String protocolName) {
        // Get the proper registry factory by protocol name
        RegistryFactory registryFactory = ServiceLoader.forClass(RegistryFactory.class).load(protocolName);
        return registryFactory;
    }
}

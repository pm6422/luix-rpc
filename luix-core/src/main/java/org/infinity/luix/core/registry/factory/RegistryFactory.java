package org.infinity.luix.core.registry.factory;


import org.infinity.luix.core.registry.Registry;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.serviceloader.ServiceLoader;
import org.infinity.luix.utilities.serviceloader.annotation.Spi;
import org.infinity.luix.utilities.serviceloader.annotation.SpiScope;

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
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static RegistryFactory getInstance(String name) {
        return ServiceLoader.forClass(RegistryFactory.class).load(name);
    }
}

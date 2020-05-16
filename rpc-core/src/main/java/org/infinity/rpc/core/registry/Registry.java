package org.infinity.rpc.core.registry;

import java.util.List;

/**
 * Registry interface
 */
public interface Registry extends Registrable, Subscribable {
    /**
     * Get registry subclass name
     *
     * @return registry subclass name
     */
    String getRegistryClassName();

    /**
     * Get registry url
     *
     * @return registry url
     */
    Url getRegistryUrl();

    List<String> discoverActiveProviderAddress(String providerPath);
}

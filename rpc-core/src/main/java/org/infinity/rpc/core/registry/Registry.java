package org.infinity.rpc.core.registry;

import org.infinity.rpc.core.url.Url;

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

    /**
     * @param providerPath
     * @return
     */
    List<String> discoverActiveProviderAddress(String providerPath);
}

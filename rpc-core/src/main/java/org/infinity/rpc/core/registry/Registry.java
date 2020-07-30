package org.infinity.rpc.core.registry;

import org.infinity.rpc.core.url.Url;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registry interface
 */
public interface Registry extends Registrable, Subscribable {

    AtomicReference<List<Url>>      REGISTRY_URL_CACHE = new AtomicReference<>();
    AtomicReference<List<Registry>> REGISTRY_CACHE     = new AtomicReference<>();

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

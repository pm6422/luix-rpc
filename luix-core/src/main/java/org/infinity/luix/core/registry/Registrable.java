package org.infinity.luix.core.registry;

import org.infinity.luix.core.url.Url;

import java.util.List;
import java.util.Set;

/**
 * The interface class defines the actions of provider
 */
public interface Registrable {
    /**
     * Register a provider url to registry
     *
     * @param providerUrl provider url
     */
    void register(Url providerUrl);

    /**
     * Unregister provider url from registry
     *
     * @param providerUrl provider url
     */
    void deregister(Url providerUrl);

    /**
     * Register the url to 'active' node of registry
     *
     * @param providerUrl provider url
     */
    void activate(Url providerUrl);

    /**
     * Register the url to 'inactive' node of registry
     *
     * @param providerUrl provider url
     */
    void deactivate(Url providerUrl);

    /**
     * Register and activate the consumer url to registry
     *
     * @param consumerUrl consumer url
     */
    void subscribe(Url consumerUrl);

    /**
     * Deregister the consumer url from registry
     *
     * @param consumerUrl consumer url
     */
    void unsubscribe(Url consumerUrl);

    /**
     * Get the registered provider urls
     *
     * @return provider urls
     */
    Set<Url> getRegisteredProviderUrls();

    /**
     * Get all provider paths
     */
    List<Url> getAllProviderUrls();

}

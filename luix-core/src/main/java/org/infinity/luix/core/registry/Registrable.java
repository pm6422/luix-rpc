package org.infinity.luix.core.registry;

import org.infinity.luix.core.url.Url;

import java.util.List;
import java.util.Set;

/**
 * The interface class defines a series of actions of registration
 */
public interface Registrable {
    /**
     * Register a provider or consumer url to registry
     *
     * @param url provider url
     */
    void register(Url url);

    /**
     * Deregister provider or consumer url from registry
     *
     * @param url provider or consumer url
     */
    void deregister(Url url);

    /**
     * Change the status of provider or consumer url to 'active'
     *
     * @param url provider or consumer url
     */
    void activate(Url url);

    /**
     * Change the status of provider or consumer url to 'inactive'
     *
     * @param url provider or consumer url
     */
    void deactivate(Url url);

    /**
     * Get the registered provider urls
     *
     * @return provider urls
     */
    Set<Url> getRegisteredProviderUrls();

    /**
     * Get the registered consumer urls
     *
     * @return provider urls
     */
    Set<Url> getRegisteredConsumerUrls();
}

package org.infinity.rpc.core.registry;

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
    void unregister(Url providerUrl);

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
     * Get the registered provider urls
     *
     * @return provider urls
     */
    Set<Url> getRegisteredProviderUrls();
    /**
     * Register application info to registry
     *
     * @param app application info
     */
    void registerApplication(App app);
}

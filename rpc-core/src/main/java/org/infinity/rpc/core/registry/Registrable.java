package org.infinity.rpc.core.registry;

import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.url.Url;

import java.util.List;
import java.util.Map;
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
     * @param application application info
     */
    void registerApplication(ApplicationExtConfig application);

    /**
     * Register application provider info to registry
     *
     * @param appName application name
     * @param providerUrl provider url
     */
    void registerApplicationProvider(String appName, Url providerUrl);
    /**
     *
     */
    List<String> getAllProviderForms();
    /**
     *
     */
    List<ApplicationExtConfig> getAllApps();
    /**
     *
     */
    Map<String, Map<String, List<AddressInfo>>> getAllProviders(String group);
}

package org.infinity.rpc.core.registry;

import org.infinity.rpc.core.config.spring.config.InfinityProperties;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Create registry urls
     *
     * @param infinityProperties configuration properties
     * @return registry urls
     */
    static List<Url> getRegistryUrls(InfinityProperties infinityProperties) {
        Url registryUrl = Url.registryUrl(infinityProperties.getRegistry().getName().getValue(),
                infinityProperties.getRegistry().getHost(),
                infinityProperties.getRegistry().getPort());

        // Assign values to parameters
        registryUrl.addParameter(Url.PARAM_CHECK_HEALTH, Url.PARAM_CHECK_HEALTH_DEFAULT_VALUE);
        registryUrl.addParameter(Url.PARAM_ADDRESS, registryUrl.getAddress());
        registryUrl.addParameter(Url.PARAM_CONNECT_TIMEOUT, infinityProperties.getRegistry().getConnectTimeout().toString());
        registryUrl.addParameter(Url.PARAM_SESSION_TIMEOUT, infinityProperties.getRegistry().getSessionTimeout().toString());
        registryUrl.addParameter(Url.PARAM_RETRY_INTERVAL, infinityProperties.getRegistry().getRetryInterval().toString());
        // TODO: Support multiple registry centers
        return Arrays.asList(registryUrl);
    }

    /**
     * @param infinityProperties
     * @return
     */
    static List<Registry> getRegistry(InfinityProperties infinityProperties) {
        List<Url> registryUrls = getRegistryUrls(infinityProperties);
        List<Registry> registries = new ArrayList<>();
        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactory = RegistryFactory.getInstance(infinityProperties.getRegistry().getName().getValue());
            registries.add(registryFactory.getRegistry(registryUrl));
        }
        return registries;
    }
}

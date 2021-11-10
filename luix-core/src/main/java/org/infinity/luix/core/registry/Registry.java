package org.infinity.luix.core.registry;

import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.server.listener.ConsumerProcessable;
import org.infinity.luix.core.subscribe.Subscribable;

import java.util.List;

/**
 * Registry interface
 */
public interface Registry extends Registrable, Subscribable {
    /**
     * Get registry type
     *
     * @return registry type
     */
    String getType();

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
     * Discover
     *
     * @param providerPath provider path
     * @return address list
     */
    List<String> discoverActiveProviderAddress(String providerPath);

    void subscribeConsumerListener(String interfaceName, ConsumerProcessable consumerProcessor);
}

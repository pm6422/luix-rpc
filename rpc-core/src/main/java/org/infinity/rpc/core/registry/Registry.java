package org.infinity.rpc.core.registry;

import org.infinity.rpc.core.server.listener.ConsumerProcessable;
import org.infinity.rpc.core.subscribe.Subscribable;
import org.infinity.rpc.core.url.Url;

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

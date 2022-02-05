package org.infinity.luix.core.subscribe;

import org.infinity.luix.core.listener.client.ProviderDiscoveryListener;
import org.infinity.luix.core.listener.client.GlobalProviderDiscoveryListener;
import org.infinity.luix.core.listener.client.GlobalConsumerDiscoveryListener;
import org.infinity.luix.core.url.Url;

import java.util.List;

/**
 * The interface class defines the actions of a client
 */
public interface Subscribable {
    /**
     * Discover all the provider urls of the consumer, including 'inactive' urls
     *
     * @param consumerUrl        consumer url
     * @param onlyFetchFromCache if true, only fetch from cache
     * @return provider urls
     */
    List<Url> discover(Url consumerUrl, boolean onlyFetchFromCache);

    /**
     * Bind a listener to a consumer
     *
     * @param consumerUrl consumer url
     * @param listener    consumer listener
     */
    void subscribe(Url consumerUrl, ProviderDiscoveryListener listener);

    /**
     * Unbind a listener from a consumer
     *
     * @param consumerUrl consumer url
     * @param listener    consumer listener
     */
    void unsubscribe(Url consumerUrl, ProviderDiscoveryListener listener);

    /**
     * Bind a listener for all consumers
     *
     * @param listener consumer listener
     */
    void subscribe(GlobalProviderDiscoveryListener listener);

    /**
     * Unbind a listener for all consumers
     *
     * @param listener consumer listener
     */
    void unsubscribe(GlobalProviderDiscoveryListener listener);

    /**
     * Subscribe consumer changes processor
     *
     * @param consumerProcessor consumer changes processor
     */
    void subscribeAllConsumerChanges(GlobalConsumerDiscoveryListener consumerProcessor);

}
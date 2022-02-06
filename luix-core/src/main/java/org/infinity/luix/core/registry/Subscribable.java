package org.infinity.luix.core.registry;

import org.infinity.luix.core.listener.GlobalConsumerDiscoveryListener;
import org.infinity.luix.core.listener.GlobalProviderDiscoveryListener;
import org.infinity.luix.core.listener.ProviderDiscoveryListener;
import org.infinity.luix.core.url.Url;

import java.util.List;

/**
 * The interface class defines the actions of consumer
 */
public interface Subscribable {
    /**
     * Discover all the provider urls, including 'inactive' urls
     *
     * @return provider urls
     */
    List<Url> discover();

    /**
     * Discover 'active' the provider urls of the specified consumer, including 'inactive' urls
     *
     * @param consumerUrl        consumer url
     * @param onlyFetchFromCache if true, only fetch from cache
     * @return provider urls
     */
    List<Url> discoverActive(Url consumerUrl, boolean onlyFetchFromCache);

    /**
     * Bind a listener to a subscriber
     *
     * @param consumerUrl consumer url
     * @param listener    listener
     */
    void subscribe(Url consumerUrl, ProviderDiscoveryListener listener);

    /**
     * Unbind a listener from a subscriber
     *
     * @param consumerUrl consumer url
     * @param listener    listener
     */
    void unsubscribe(Url consumerUrl, ProviderDiscoveryListener listener);

    /**
     * Bind a global listener for all subscribers
     *
     * @param listener listener
     */
    void subscribe(GlobalProviderDiscoveryListener listener);

    /**
     * Unbind a listener for all subscribers
     *
     * @param listener listener
     */
    void unsubscribe(GlobalProviderDiscoveryListener listener);

    /**
     * Bind a global listener for all subscribers
     *
     * @param listener listener
     */
    void subscribe(GlobalConsumerDiscoveryListener listener);

}
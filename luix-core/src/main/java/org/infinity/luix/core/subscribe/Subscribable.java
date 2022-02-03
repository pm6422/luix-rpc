package org.infinity.luix.core.subscribe;

import org.infinity.luix.core.registry.listener.ClientListener;
import org.infinity.luix.core.server.listener.ConsumerProcessable;
import org.infinity.luix.core.url.Url;

import java.util.List;

/**
 * The interface class defines the actions of a client
 */
public interface Subscribable {
    /**
     * Discover all the provider urls of the client, including 'inactive' urls
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    List<Url> discover(Url consumerUrl);

    /**
     * Bind a listener to a client
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    void subscribe(Url consumerUrl, ClientListener listener);

    /**
     * Unbind a listener from a client
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    void unsubscribe(Url consumerUrl, ClientListener listener);

    /**
     * @param interfaceName
     * @param consumerProcessor
     */
    void subscribeConsumerListener(String interfaceName, ConsumerProcessable consumerProcessor);

}
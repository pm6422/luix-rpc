package org.infinity.luix.core.subscribe;

import org.infinity.luix.core.listener.client.ConsumerListener;
import org.infinity.luix.core.listener.client.ConsumersListener;
import org.infinity.luix.core.listener.server.ConsumerProcessable;
import org.infinity.luix.core.url.Url;

import java.util.List;

/**
 * The interface class defines the actions of a client
 */
public interface Subscribable {
    /**
     * Discover all the provider urls of the consumer, including 'inactive' urls
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    List<Url> discover(Url consumerUrl);

    /**
     * Bind a listener to a consumer
     *
     * @param consumerUrl consumer url
     * @param listener    consumer listener
     */
    void subscribe(Url consumerUrl, ConsumerListener listener);

    /**
     * Unbind a listener from a consumer
     *
     * @param consumerUrl consumer url
     * @param listener    consumer listener
     */
    void unsubscribe(Url consumerUrl, ConsumerListener listener);

    /**
     * Bind a listener for all consumers
     *
     * @param listener consumer listener
     */
    void subscribe(ConsumersListener listener);

    /**
     * Unbind a listener for all consumers
     *
     * @param listener consumer listener
     */
    void unsubscribe(ConsumersListener listener);

    /**
     * Subscribe consumer changes processor
     *
     * @param consumerProcessor consumer changes processor
     */
    void subscribeAllConsumerChanges(ConsumerProcessable consumerProcessor);

}
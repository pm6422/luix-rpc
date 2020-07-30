package org.infinity.rpc.core.subscribe;

import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;

import java.util.List;

/**
 * The interface class defines the actions of a client
 */
public interface Subscribable {
    /**
     * Discover all the provider urls of the client, including 'inactive' urls
     *
     * @param clientUrl client url
     * @return provider urls
     */
    List<Url> discover(Url clientUrl);

    /**
     * Bind a listener to a client
     *
     * @param clientUrl client url
     * @param listener  client listener
     */
    void subscribe(Url clientUrl, ClientListener listener);

    /**
     * Unbind a listener from a client
     *
     * @param clientUrl client url
     * @param listener  client listener
     */
    void unsubscribe(Url clientUrl, ClientListener listener);
}
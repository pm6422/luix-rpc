package org.infinity.rpc.core.client.request;

import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

/**
 * The initiator of the RPC request
 * It used to call the RPC provider
 *
 * @param <T>: The interface class of the provider
 */
public interface Importable<T> {

    /**
     * Get provider url
     *
     * @return provider url
     */
    Url getProviderUrl();

    /**
     * Check whether it is available
     *
     * @return true: available, false: unavailable
     */
    boolean isActive();

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable call(Requestable request);

    /**
     * Do some cleanup task
     */
    void destroy();

}

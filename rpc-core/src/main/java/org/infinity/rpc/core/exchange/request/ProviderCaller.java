package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;

/**
 * The initiator of the RPC request
 * It used to call the RPC provider
 *
 *
 * @param <T>: The interface class of the provider
 */
public interface ProviderCaller<T> {

    /**
     * Get provider url
     *
     * @return provider url
     */
    Url getProviderUrl();

    /**
     * Get provider interface class
     *
     * @return interface class
     */
    Class<T> getInterfaceClass();

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

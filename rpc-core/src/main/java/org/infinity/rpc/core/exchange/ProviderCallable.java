package org.infinity.rpc.core.exchange;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;

/**
 * @param <T>: The interface class of the provider
 */
public interface ProviderCallable<T> {
    /**
     * Initialize
     */
    void init();

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
    boolean isAvailable();

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

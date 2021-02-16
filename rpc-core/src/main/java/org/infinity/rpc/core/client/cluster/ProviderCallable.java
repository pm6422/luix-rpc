package org.infinity.rpc.core.client.cluster;

import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;

/**
 * todo: merge to ProviderCluster
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
    boolean isActive();

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable call(Requestable request);
}

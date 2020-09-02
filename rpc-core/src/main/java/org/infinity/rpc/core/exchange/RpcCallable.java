package org.infinity.rpc.core.exchange;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;

public interface RpcCallable<T> {
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
    Responseable call(Requestable<T> request);

    /**
     * Initialize
     */
    void init();

    /**
     * Do some cleanup task
     */
    void destroy();
}

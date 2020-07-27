package org.infinity.rpc.core.exchange;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;

public interface RpcCallable<T> {
    /**
     * @return
     */
    Class<T> getInterfaceClass();

    /**
     * @return
     */
    boolean isAvailable();

    /**
     * @return
     */
    Url getProviderUrl();

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable call(Requestable request);

    /**
     *
     */
    void init();

    void destroy();
}

package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;

/**
 * The initiator of the RPC request
 */
public interface Requester<T> {
    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable call(Requestable request);

    /**
     *
     * @return
     */
    Url getUrl();

    /**
     *
     * @return
     */
    boolean isAvailable();
}

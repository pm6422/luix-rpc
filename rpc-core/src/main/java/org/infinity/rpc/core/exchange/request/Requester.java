package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.response.Responseable;

/**
 * The initiator of the RPC request
 */
public interface Requester {
    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable call(Requestable request);
}

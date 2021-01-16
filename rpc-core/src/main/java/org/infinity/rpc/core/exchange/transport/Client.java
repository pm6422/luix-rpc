package org.infinity.rpc.core.exchange.transport;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.transport.endpoint.Endpoint;

public interface Client extends Endpoint {
    /**
     * async send request
     *
     * @param request request object
     */
    void checkHealth(Requestable request);
}

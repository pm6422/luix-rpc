package org.infinity.luix.core.exchange.client;

import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.exchange.endpoint.Endpoint;

public interface Client extends Endpoint {
    /**
     * async send request
     *
     * @param request request object
     */
    void checkHealth(Requestable request);
}

package com.luixtech.luixrpc.core.exchange.client;

import com.luixtech.luixrpc.core.client.request.Requestable;
import com.luixtech.luixrpc.core.exchange.endpoint.Endpoint;

public interface Client extends Endpoint {
    /**
     * async send request
     *
     * @param request request object
     */
    void checkHealth(Requestable request);
}

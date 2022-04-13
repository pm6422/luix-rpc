package com.luixtech.rpc.core.exchange.client;

import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.exchange.endpoint.Endpoint;

public interface Client extends Endpoint {
    /**
     * async send request
     *
     * @param request request object
     */
    void checkHealth(Requestable request);
}

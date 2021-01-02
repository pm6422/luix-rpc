package org.infinity.rpc.core.exchange.transport;

import org.infinity.rpc.core.exchange.request.impl.RpcRequest;

public interface Client extends Channel {
    /**
     * async send request.
     *
     * @param request RPC request
     */
    void heartbeat(RpcRequest request);
}

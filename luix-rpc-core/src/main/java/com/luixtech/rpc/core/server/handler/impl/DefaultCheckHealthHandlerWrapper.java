package com.luixtech.rpc.core.server.handler.impl;

import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.exchange.Channel;
import com.luixtech.rpc.core.server.handler.InvocationHandleable;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.rpc.core.server.response.impl.RpcCheckHealthResponse;

import static com.luixtech.rpc.core.client.request.impl.RpcCheckHealthRequest.isCheckHealthRequest;

public class DefaultCheckHealthHandlerWrapper implements InvocationHandleable {
    private final InvocationHandleable serverInvocationHandleable;

    public DefaultCheckHealthHandlerWrapper(InvocationHandleable serverInvocationHandleable) {
        this.serverInvocationHandleable = serverInvocationHandleable;
    }

    @Override
    public Object handle(Channel channel, Object message) {
        if (isCheckHealthRequest(message)) {
            // Check health short circuit handling can accelerate speed
            return handleCheckHealthRequest(message);
        }
        return serverInvocationHandleable.handle(channel, message);
    }

    private Responseable handleCheckHealthRequest(Object message) {
        Requestable request = (Requestable) message;
        Responseable response = RpcCheckHealthResponse.of(request.getRequestId());
        response.setProtocolVersion(request.getProtocolVersion());
        return response;
    }
}
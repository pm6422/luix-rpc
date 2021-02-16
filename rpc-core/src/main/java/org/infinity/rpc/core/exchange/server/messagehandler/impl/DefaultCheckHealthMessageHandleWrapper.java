package org.infinity.rpc.core.exchange.server.messagehandler.impl;

import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcCheckHealthResponse;
import org.infinity.rpc.core.exchange.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.exchange.transport.Channel;

import static org.infinity.rpc.core.client.request.impl.RpcCheckHealthRequest.isCheckHealthRequest;

public class DefaultCheckHealthMessageHandleWrapper implements MessageHandler {
    private final MessageHandler messageHandler;

    public DefaultCheckHealthMessageHandleWrapper(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public Object handle(Channel channel, Object message) {
        if (isCheckHealthRequest(message)) {
            // Check health short circuit handling can accelerate speed
            return handleCheckHealthRequest(message);
        }
        return messageHandler.handle(channel, message);
    }

    private Responseable handleCheckHealthRequest(Object message) {
        Requestable request = (Requestable) message;
        Responseable response = new RpcCheckHealthResponse(request.getRequestId());
        response.setProtocolVersion(request.getProtocolVersion());
        return response;
    }
}
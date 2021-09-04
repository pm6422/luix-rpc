package org.infinity.luix.core.server.messagehandler.impl;

import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.exchange.Channel;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.core.server.response.impl.RpcCheckHealthResponse;
import org.infinity.luix.core.server.messagehandler.MessageHandler;

import static org.infinity.luix.core.client.request.impl.RpcCheckHealthRequest.isCheckHealthRequest;

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
        Responseable response = RpcCheckHealthResponse.of(request.getRequestId());
        response.setProtocolVersion(request.getProtocolVersion());
        return response;
    }
}
package org.infinity.rpc.core.config.spring.server.messagehandler.impl;

import org.infinity.rpc.core.config.spring.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.impl.RpcHealthRequest;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcHealthResponse;
import org.infinity.rpc.core.exchange.transport.Channel;

import static org.infinity.rpc.core.exchange.transport.checkhealth.impl.DefaultCheckHealthFactory.*;

public class DefaultCheckHealthMessageHandleWrapper implements MessageHandler {
    private final MessageHandler messageHandler;

    public DefaultCheckHealthMessageHandleWrapper(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public Object handle(Channel channel, Object message) {
        if (isCheckHealthRequest(message)) {
            Responseable response = createResponse(((Requestable) message).getRequestId());
            response.setProtocolVersion(((Requestable) message).getProtocolVersion());
            return response;
        }
        return messageHandler.handle(channel, message);
    }

    public static boolean isCheckHealthRequest(Object message) {
        if (!(message instanceof Requestable)) {
            return false;
        }
        if (message instanceof RpcHealthRequest) {
            return true;
        }
        Requestable request = (Requestable) message;
        return CHECK_HEALTH_INTERFACE_NAME.equals(request.getInterfaceName())
                && CHECK_HEALTH_METHOD_NAME.equals(request.getMethodName())
                && CHECK_HEALTH_METHOD_PARAM.endsWith(request.getMethodParameters());
    }

    public static boolean isCheckHealthResponse(Object message) {
        return message instanceof RpcHealthResponse;
    }

    public static Responseable createResponse(long requestId) {
        return new RpcHealthResponse(requestId, CHECK_HEALTH_RETURN);
    }
}
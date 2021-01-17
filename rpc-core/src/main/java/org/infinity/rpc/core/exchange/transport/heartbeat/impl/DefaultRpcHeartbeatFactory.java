/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.rpc.core.exchange.transport.heartbeat.impl;

import org.infinity.rpc.core.config.spring.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.impl.RpcHealthRequest;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcHealthResponse;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.exchange.transport.heartbeat.HeartbeatFactory;
import org.infinity.rpc.core.utils.MethodParameterUtils;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

/**
 *
 */
@ServiceName("default")
public class DefaultRpcHeartbeatFactory implements HeartbeatFactory {
    /**
     * heartbeat constants start
     */
    public static final String CHECK_HEALTH_INTERFACE_NAME = "org.infinity.rpc.health.Checker";
    public static final String CHECK_HEALTH_METHOD_NAME    = "checkHealth";
    public static final String CHECK_HEALTH_METHOD_PARAM   = MethodParameterUtils.VOID;
    public static final String CHECK_HEALTH_RETURN         = "SUCCESS";

    @Override
    public MessageHandler wrapMessageHandler(MessageHandler handler) {
        return new HeartMessageHandleWrapper(handler);
    }

    @Override
    public Requestable createRequest() {
        return new RpcHealthRequest(IdGenerator.generateTimestampId(),
                CHECK_HEALTH_INTERFACE_NAME, CHECK_HEALTH_METHOD_NAME, CHECK_HEALTH_METHOD_PARAM);
    }

    public static Responseable createResponse(long requestId) {
        RpcHealthResponse response = new RpcHealthResponse();
        response.setRequestId(requestId);
        response.setResult(CHECK_HEALTH_RETURN);
        return response;
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

    private static class HeartMessageHandleWrapper implements MessageHandler {
        private final MessageHandler messageHandler;

        public HeartMessageHandleWrapper(MessageHandler messageHandler) {
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
    }
}

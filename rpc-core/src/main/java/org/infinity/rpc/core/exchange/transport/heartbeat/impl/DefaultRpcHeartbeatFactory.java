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

import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.impl.RpcRequest;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcResponse;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.config.spring.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.exchange.transport.heartbeat.HeartbeatFactory;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

/**
 *
 */
@ServiceName("default")
public class DefaultRpcHeartbeatFactory implements HeartbeatFactory {

    @Override
    public Requestable createRequest() {
        return getDefaultHeartbeatRequest(IdGenerator.generateTimestampId());
    }

    @Override
    public MessageHandler wrapMessageHandler(MessageHandler handler) {
        return new HeartMessageHandleWrapper(handler);
    }

    public static Requestable getDefaultHeartbeatRequest(long requestId) {
        HeartbeatRequest request = new HeartbeatRequest();

        request.setRequestId(requestId);
        request.setInterfaceName(RpcConstants.HEARTBEAT_INTERFACE_NAME);
        request.setMethodName(RpcConstants.HEARTBEAT_METHOD_NAME);
        request.setMethodParameters(RpcConstants.HEARTBEAT_PARAM);

        return request;
    }

    public static boolean isHeartbeatRequest(Object message) {
        if (!(message instanceof Requestable)) {
            return false;
        }
        if (message instanceof HeartbeatRequest) {
            return true;
        }

        Requestable request = (Requestable) message;

        return RpcConstants.HEARTBEAT_INTERFACE_NAME.equals(request.getInterfaceName())
                && RpcConstants.HEARTBEAT_METHOD_NAME.equals(request.getMethodName())
                && RpcConstants.HEARTBEAT_PARAM.endsWith(request.getMethodParameters());
    }

    public static Responseable getDefaultHeartbeatResponse(long requestId) {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setRequestId(requestId);
        response.setResult("heartbeat");
        return response;
    }

    public static boolean isHeartbeatResponse(Object message) {
        return message instanceof HeartbeatResponse;
    }


    private class HeartMessageHandleWrapper implements MessageHandler {
        private MessageHandler messageHandler;

        public HeartMessageHandleWrapper(MessageHandler messageHandler) {
            this.messageHandler = messageHandler;
        }

        @Override
        public Object handle(Channel channel, Object message) {
            if (isHeartbeatRequest(message)) {
                Responseable response = getDefaultHeartbeatResponse(((Requestable) message).getRequestId());
                response.setProtocolVersion(((Requestable) message).getProtocolVersion());
                return response;
            }
            return messageHandler.handle(channel, message);
        }
    }

    static class HeartbeatRequest extends RpcRequest {
        private static final long serialVersionUID = -9091565508658370712L;
    }

    static class HeartbeatResponse extends RpcResponse {
        private static final long serialVersionUID = -3894152771593026692L;
    }
}

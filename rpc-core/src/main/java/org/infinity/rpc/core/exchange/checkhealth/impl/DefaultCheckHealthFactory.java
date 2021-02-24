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

package org.infinity.rpc.core.exchange.checkhealth.impl;

import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.request.impl.RpcCheckHealthRequest;
import org.infinity.rpc.core.exchange.checkhealth.CheckHealthFactory;
import org.infinity.rpc.core.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.server.messagehandler.impl.DefaultCheckHealthMessageHandleWrapper;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import static org.infinity.rpc.core.constant.ServiceConstants.CHECK_HEALTH_FACTORY_VAL_DEFAULT;

/**
 *
 */
@SpiName(CHECK_HEALTH_FACTORY_VAL_DEFAULT)
public class DefaultCheckHealthFactory implements CheckHealthFactory {
    @Override
    public MessageHandler wrapMessageHandler(MessageHandler handler) {
        return new DefaultCheckHealthMessageHandleWrapper(handler);
    }

    @Override
    public Requestable createRequest() {
        return new RpcCheckHealthRequest(IdGenerator.generateTimestampId());
    }
}

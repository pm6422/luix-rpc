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

package org.infinity.rpc.core.exchange.transport.heartbeat;


import org.infinity.rpc.core.config.spring.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

/**
 * heartbeat的消息保持和正常请求的Request一致，这样以便更能反应service端的可用情况
 */
@Spi(scope = SpiScope.SINGLETON)
public interface HeartbeatFactory {

    /**
     * 创建心跳包
     *
     * @return
     */
    Requestable createRequest();

    /**
     * 包装 handler，支持心跳包的处理
     *
     * @param handler
     * @return
     */
    MessageHandler wrapMessageHandler(MessageHandler handler);

    /**
     * Get heartbeat factory instance associated with the specified name
     *
     * @param name specified heartbeat factory name
     * @return heartbeat factory instance
     */
    static HeartbeatFactory getInstance(String name) {
        return ServiceLoader.forClass(HeartbeatFactory.class).load(name);
    }
}

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

package org.infinity.rpc.core.exchange.transport.checkhealth;


import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exchange.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.util.Optional;

import static org.infinity.rpc.core.constant.ServiceConstants.CHECK_HEALTH_FACTORY;

@Spi(scope = SpiScope.SINGLETON)
public interface CheckHealthFactory {

    /**
     * Create check health request object
     *
     * @return request object
     */
    Requestable createRequest();

    /**
     * Wrap message handler in order to support check health
     *
     * @param handler message handler
     * @return wrapped message handler
     */
    MessageHandler wrapMessageHandler(MessageHandler handler);

    /**
     * Get check health factory instance associated with the specified name
     *
     * @param name specified check health factory name
     * @return check health factory instance
     */
    static CheckHealthFactory getInstance(String name) {
        return Optional.ofNullable(ServiceLoader.forClass(CheckHealthFactory.class).load(name))
                .orElseThrow(() -> new RpcFrameworkException("No check health factory [" + name + "] found!"));
    }

    /**
     * Get check health factory instance associated with the specified name
     *
     * @param providerUrl provider url
     * @return check health factory instance
     */
    static CheckHealthFactory getInstance(Url providerUrl) {
        final String name = providerUrl.getOption(CHECK_HEALTH_FACTORY);
        return Optional.ofNullable(ServiceLoader.forClass(CheckHealthFactory.class).load(name))
                .orElseThrow(() -> new RpcFrameworkException("No check health factory [" + name + "] found!"));
    }
}

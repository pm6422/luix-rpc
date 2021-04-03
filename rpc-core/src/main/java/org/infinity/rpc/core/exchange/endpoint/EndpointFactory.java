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

package org.infinity.rpc.core.exchange.endpoint;

import org.infinity.rpc.core.exchange.client.Client;
import org.infinity.rpc.core.exchange.server.Server;
import org.infinity.rpc.core.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface EndpointFactory {

    /**
     * create remote server
     *
     * @param providerUrl    provider url
     * @param messageHandler message handler
     * @return server
     */
    Server createServer(Url providerUrl, MessageHandler messageHandler);

    /**
     * create remote client
     *
     * @param providerUrl provider url
     * @return client
     */
    Client createClient(Url providerUrl);

    /**
     * safe release server
     *
     * @param server      server
     * @param providerUrl provider url
     */
    void safeReleaseResource(Server server, Url providerUrl);

    /**
     * safe release client
     *
     * @param client      client
     * @param providerUrl provider url
     */
    void safeReleaseResource(Client client, Url providerUrl);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static EndpointFactory getInstance(String name) {
        return ServiceLoader.forClass(EndpointFactory.class).load(name);
    }
}

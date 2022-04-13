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

package com.luixtech.rpc.core.exchange.endpoint;

import com.luixtech.rpc.core.exception.impl.RpcConfigException;
import com.luixtech.rpc.core.exchange.client.Client;
import com.luixtech.rpc.core.exchange.server.Server;
import com.luixtech.rpc.core.server.handler.InvocationHandleable;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import com.luixtech.utilities.serviceloader.annotation.Spi;
import com.luixtech.utilities.serviceloader.annotation.SpiScope;

import java.util.Optional;

@Spi(scope = SpiScope.SINGLETON)
public interface NetworkTransmissionFactory {

    /**
     * Create remote client
     *
     * @param providerUrl provider url
     * @return client
     */
    Client createClient(Url providerUrl);

    /**
     * Destroy client
     *
     * @param client      client
     * @param providerUrl provider url
     */
    void destroyClient(Client client, Url providerUrl);

    /**
     * Create remote server
     *
     * @param providerUrl               provider url
     * @param providerInvocationHandler provider invocation handler
     * @return server
     */
    Server createServer(Url providerUrl, InvocationHandleable providerInvocationHandler);

    /**
     * Destroy server
     *
     * @param server      server
     * @param providerUrl provider url
     */
    void destroyServer(Server server, Url providerUrl);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static NetworkTransmissionFactory getInstance(String name) {
        return Optional.ofNullable(ServiceLoader.forClass(NetworkTransmissionFactory.class).load(name))
                .orElseThrow(() -> new RpcConfigException("Endpoint factory [" + name + "] does NOT exist, " +
                        "please check whether the correct dependency is in your class path!"));
    }
}

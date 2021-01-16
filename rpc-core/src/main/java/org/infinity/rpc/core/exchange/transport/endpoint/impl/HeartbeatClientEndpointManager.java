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

package org.infinity.rpc.core.exchange.transport.endpoint.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.transport.Client;
import org.infinity.rpc.core.exchange.transport.endpoint.Endpoint;
import org.infinity.rpc.core.exchange.transport.endpoint.EndpointManager;
import org.infinity.rpc.core.exchange.transport.heartbeat.HeartbeatFactory;
import org.infinity.rpc.core.url.Url;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.infinity.rpc.core.destroy.ScheduledThreadPool.CHECK_HEALTH_THREAD_POOL;

/**
 *
 */
@Slf4j
public class HeartbeatClientEndpointManager implements EndpointManager {

    private final Map<Client, HeartbeatFactory> endpoints = new ConcurrentHashMap<>();
    private       ScheduledExecutorService      executorService;

    @Override
    public void init() {
        executorService = ScheduledThreadPool.schedulePeriodicalTask(CHECK_HEALTH_THREAD_POOL,
                RpcConstants.HEARTBEAT_PERIOD, RpcConstants.HEARTBEAT_PERIOD, TimeUnit.MILLISECONDS, () -> {
                    for (Map.Entry<Client, HeartbeatFactory> endpoint : endpoints.entrySet()) {
                        Client client = endpoint.getKey();
                        try {
                            if (client.isActive()) {
                                // Skip health check process if current endpoint is active
                                continue;
                            }
                            HeartbeatFactory factory = endpoint.getValue();
                            client.heartbeat(factory.createRequest());
                        } catch (Exception e) {
                            log.error("Failed to check health for provider url [" + client.getProviderUrl().getUri() + "]", e);
                        }
                    }
                });
    }

    @Override
    public void addEndpoint(Endpoint endpoint) {
        if (!(endpoint instanceof Client)) {
            throw new RpcFrameworkException("Failed to add endpoint [" + endpoint.getClass() + "]");
        }
        Client client = (Client) endpoint;
        Url providerUrl = endpoint.getProviderUrl();
        String heartbeatFactoryName = providerUrl.getParameter(Url.PARAM_HEART_BEAT_FACTORY, Url.PARAM_HEART_BEAT_FACTORY_DEFAULT_VALUE);
        HeartbeatFactory heartbeatFactory = HeartbeatFactory.getInstance(heartbeatFactoryName);
        if (heartbeatFactory == null) {
            throw new RpcFrameworkException("HeartbeatFactory not exist: " + heartbeatFactoryName);
        }
        endpoints.put(client, heartbeatFactory);
    }

    @Override
    public void removeEndpoint(Endpoint endpoint) {
        endpoints.remove(endpoint);
    }

    public Set<Client> getClients() {
        return Collections.unmodifiableSet(endpoints.keySet());
    }

    @Override
    public void destroy() {
        executorService.shutdownNow();
    }
}

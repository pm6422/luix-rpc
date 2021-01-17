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
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.transport.Client;
import org.infinity.rpc.core.exchange.transport.constants.ChannelState;
import org.infinity.rpc.core.exchange.transport.endpoint.Endpoint;
import org.infinity.rpc.core.exchange.transport.endpoint.EndpointManager;
import org.infinity.rpc.core.exchange.transport.heartbeat.HeartbeatFactory;
import org.infinity.rpc.core.url.Url;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.destroy.ScheduledThreadPool.CHECK_HEALTH_THREAD_POOL;

/**
 *
 */
@Slf4j
public class HeartbeatClientEndpointManager implements EndpointManager {

    /**
     * Check health interval in milliseconds
     * todo: move to global config
     */
    private static final int                           CHECK_HEALTH_TIMER_INTERVAL = 5000;
    private final        Map<Client, HeartbeatFactory> endpoints                   = new ConcurrentHashMap<>();

    @Override
    public void init() {
        ScheduledThreadPool.schedulePeriodicalTask(CHECK_HEALTH_THREAD_POOL, CHECK_HEALTH_TIMER_INTERVAL, this::checkHealth);
    }

    private void checkHealth() {
//        for (Map.Entry<Client, HeartbeatFactory> endpoint : endpoints.entrySet()) {
//            Client client = endpoint.getKey();
//            try {
//                if (isSkipCheckHealthState(client)) {
//                    log.debug("Skip checking health for url [{}] with state [{}]",
//                            client.getProviderUrl().getUri(), client.getState().name());
//                    continue;
//                }
//                HeartbeatFactory heartbeatFactory = endpoint.getValue();
//                client.checkHealth(heartbeatFactory.createRequest());
//            } catch (Exception e) {
//                log.error("Failed to check health for provider url [" + client.getProviderUrl().getUri() + "]", e);
//            }
//        }
    }

    private boolean isSkipCheckHealthState(Client client) {
        // Skip health check process if current endpoint is uninitialized or closed
        return ChannelState.UNINITIALIZED.equals(client.getState()) || client.isClosed();
//        return client.isActive() || ChannelState.UNINITIALIZED.equals(client.getState()) || client.isClosed();
    }

    @Override
    public void addEndpoint(Endpoint endpoint) {
        if (!(endpoint instanceof Client)) {
            throw new RpcFrameworkException("Failed to add endpoint [" + endpoint.getClass().getSimpleName() + "]");
        }
        Client client = (Client) endpoint;
        Url providerUrl = endpoint.getProviderUrl();
        String heartbeatFactoryName = providerUrl.getParameter(Url.PARAM_CHECK_HEALTH_FACTORY);
        HeartbeatFactory heartbeatFactory = HeartbeatFactory.getInstance(heartbeatFactoryName);
        if (heartbeatFactory == null) {
            throw new RpcFrameworkException("No check health factory [" + heartbeatFactoryName + "]");
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
        ScheduledThreadPool.shutdownNow(CHECK_HEALTH_THREAD_POOL);
    }
}

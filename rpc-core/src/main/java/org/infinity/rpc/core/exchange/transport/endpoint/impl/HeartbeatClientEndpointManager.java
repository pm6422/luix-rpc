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
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.transport.Client;
import org.infinity.rpc.core.exchange.transport.endpoint.Endpoint;
import org.infinity.rpc.core.exchange.transport.endpoint.EndpointManager;
import org.infinity.rpc.core.exchange.transport.heartbeat.HeartbeatFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.infinity.rpc.utilities.spi.ServiceLoader;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 *
 */
@Slf4j
public class HeartbeatClientEndpointManager implements EndpointManager {

    private ConcurrentMap<Client, HeartbeatFactory> endpoints = new ConcurrentHashMap<>();

    // 一般这个类创建的实例会比较少，如果共享的话，容易“被影响”，如果某个任务阻塞了
    private ScheduledExecutorService executorService = null;

    @Override
    public void init() {
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleWithFixedDelay(() -> {
            for (Map.Entry<Client, HeartbeatFactory> entry : endpoints.entrySet()) {
                Client endpoint = entry.getKey();
                try {
                    // 如果节点是存活状态，那么没必要走心跳
                    if (endpoint.isActive()) {
                        continue;
                    }
                    HeartbeatFactory factory = entry.getValue();
                    endpoint.heartbeat(factory.createRequest());
                } catch (Exception e) {
                    log.error("HeartbeatEndpointManager send heartbeat Error: url=" + endpoint.getUrl().getUri() + ", " + e.getMessage());
                }
            }
        }, RpcConstants.HEARTBEAT_PERIOD, RpcConstants.HEARTBEAT_PERIOD, TimeUnit.MILLISECONDS);

        ShutdownHook.add(() -> {
            if (!executorService.isShutdown()) {
                executorService.shutdown();
            }
        });
    }

    @Override
    public void destroy() {
        executorService.shutdownNow();
    }

    @Override
    public void addEndpoint(Endpoint endpoint) {
        if (!(endpoint instanceof Client)) {
            throw new RpcFrameworkException("HeartbeatClientEndpointManager addEndpoint Error: class not support " + endpoint.getClass());
        }

        Client client = (Client) endpoint;
        Url url = endpoint.getUrl();
        String heartbeatFactoryName = url.getParameter(Url.PARAM_HEART_BEAT_FACTORY, Url.PARAM_HEART_BEAT_FACTORY_DEFAULT_VALUE);
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
}

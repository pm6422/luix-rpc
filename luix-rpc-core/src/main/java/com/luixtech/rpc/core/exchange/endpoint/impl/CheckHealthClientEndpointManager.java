package com.luixtech.rpc.core.exchange.endpoint.impl;

import com.luixtech.rpc.core.constant.ProviderConstants;
import com.luixtech.rpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.rpc.core.exchange.checkhealth.HealthChecker;
import com.luixtech.rpc.core.exchange.client.Client;
import com.luixtech.rpc.core.exchange.constants.ChannelState;
import com.luixtech.rpc.core.exchange.endpoint.Endpoint;
import com.luixtech.rpc.core.exchange.endpoint.EndpointManager;
import com.luixtech.rpc.core.thread.ScheduledThreadPool;
import com.luixtech.rpc.core.url.Url;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.luixtech.rpc.metrics.MetricsUtils.getMemoryStatistic;


/**
 *
 */
@Slf4j
public class CheckHealthClientEndpointManager implements EndpointManager {

    /**
     * Check health interval in milliseconds
     * todo: move to global config
     */
    private final Map<Client, HealthChecker> endpoints = new ConcurrentHashMap<>();

    @Override
    public void init() {
        ScheduledThreadPool.schedulePeriodicalTask(ScheduledThreadPool.CALCULATE_MEMORY_THREAD_POOL, ScheduledThreadPool.CALCULATE_MEMORY_INTERVAL,
                () -> log.info("Memory usage: {} ", getMemoryStatistic()));
        ScheduledThreadPool.schedulePeriodicalTask(ScheduledThreadPool.CHECK_HEALTH_THREAD_POOL, ScheduledThreadPool.CHECK_HEALTH_INTERVAL, this::checkHealth);
    }

    private void checkHealth() {
        for (Map.Entry<Client, HealthChecker> endpoint : endpoints.entrySet()) {
            Client client = endpoint.getKey();
            try {
                if (isSkipCheckHealthState(client)) {
//                    log.debug("Skip checking health for url [{}] with state [{}]",
//                            client.getProviderUrl().getUri(), client.getState().name());
                    continue;
                }
                HealthChecker healthChecker = endpoint.getValue();
                client.checkHealth(healthChecker.createRequest());
            } catch (Exception e) {
                log.error("Failed to check health for provider url [" + client.getProviderUrl().getUri() + "]", e);
            }
        }
    }

    private boolean isSkipCheckHealthState(Client client) {
        // Skip health check process if current endpoint is uninitialized or closed
//        return ChannelState.CREATED.equals(client.getState()) || client.isClosed();
        return client.isActive() || ChannelState.CREATED.equals(client.getState()) || client.isClosed();
    }

    @Override
    public void addEndpoint(Endpoint endpoint) {
        if (!(endpoint instanceof Client)) {
            throw new RpcFrameworkException("Failed to add endpoint [" + endpoint.getClass().getSimpleName() + "]");
        }
        Client client = (Client) endpoint;
        Url providerUrl = endpoint.getProviderUrl();
        String heartbeatFactoryName = providerUrl.getOption(ProviderConstants.HEALTH_CHECKER, ProviderConstants.HEALTH_CHECKER_VAL_V1);
        HealthChecker healthChecker = HealthChecker.getInstance(heartbeatFactoryName);
        endpoints.put(client, healthChecker);
    }

    @Override
    public void removeEndpoint(Endpoint endpoint) {
        endpoints.remove((Client) endpoint);
    }

    public Set<Client> getClients() {
        return Collections.unmodifiableSet(endpoints.keySet());
    }

    @Override
    public void destroy() {
        ScheduledThreadPool.shutdownNow(ScheduledThreadPool.CHECK_HEALTH_THREAD_POOL);
    }
}

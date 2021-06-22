package org.infinity.rpc.core.exchange.endpoint.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.exchange.checkhealth.HealthChecker;
import org.infinity.rpc.core.exchange.client.Client;
import org.infinity.rpc.core.exchange.constants.ChannelState;
import org.infinity.rpc.core.exchange.endpoint.Endpoint;
import org.infinity.rpc.core.exchange.endpoint.EndpointManager;
import org.infinity.rpc.core.thread.ScheduledThreadPool;
import org.infinity.rpc.core.url.Url;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.ProviderConstants.HEALTH_CHECKER;
import static org.infinity.rpc.core.constant.ProviderConstants.HEALTH_CHECKER_VAL_DEFAULT;
import static org.infinity.rpc.core.thread.ScheduledThreadPool.*;
import static org.infinity.rpc.utilities.StatisticsUtils.calculateMemory;

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
        ScheduledThreadPool.schedulePeriodicalTask(CALCULATE_MEMORY_THREAD_POOL, CALCULATE_MEMORY_INTERVAL,
                () -> log.info("Memory usage: {} ", calculateMemory()));
        ScheduledThreadPool.schedulePeriodicalTask(CHECK_HEALTH_THREAD_POOL, CHECK_HEALTH_INTERVAL, this::checkHealth);
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
//        return ChannelState.UNINITIALIZED.equals(client.getState()) || client.isClosed();
        return client.isActive() || ChannelState.UNINITIALIZED.equals(client.getState()) || client.isClosed();
    }

    @Override
    public void addEndpoint(Endpoint endpoint) {
        if (!(endpoint instanceof Client)) {
            throw new RpcFrameworkException("Failed to add endpoint [" + endpoint.getClass().getSimpleName() + "]");
        }
        Client client = (Client) endpoint;
        Url providerUrl = endpoint.getProviderUrl();
        String heartbeatFactoryName = providerUrl.getOption(HEALTH_CHECKER, HEALTH_CHECKER_VAL_DEFAULT);
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
        ScheduledThreadPool.shutdownNow(CHECK_HEALTH_THREAD_POOL);
    }
}

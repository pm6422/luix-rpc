package com.luixtech.luixrpc.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.registry.consul.utils.ConsulUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.luixtech.luixrpc.registry.consul.ConsulService.TAG_PREFIX_PATH;
import static com.luixtech.luixrpc.registry.consul.utils.ConsulUtils.SEMICOLON;

@Slf4j
public class ConsulHttpClient {
    /**
     * Consul query timeout in seconds
     */
    public static        long         CONSUL_QUERY_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(9);
    /**
     * Service instance ID prefix
     */
    private static final String       SERVICE_INSTANCE_ID_PREFIX   = "service:";
    /**
     * Consul client instance
     */
    public static        ConsulClient consulClient;

    public ConsulHttpClient(String host, int port) {
        consulClient = new ConsulClient(host, port);
        log.info("Created consul client with host: [{}] and port: [{}]", host, port);
    }

    public void registerService(ConsulService service) {
        consulClient.agentServiceRegister(service.toNewService());
    }

    public void deregisterService(String serviceInstanceId) {
        consulClient.agentServiceDeregister(serviceInstanceId);
    }

    public void activate(String serviceInstanceId) {
        consulClient.agentCheckPass(SERVICE_INSTANCE_ID_PREFIX + serviceInstanceId);
    }

    public void deactivate(String serviceInstanceId) {
        consulClient.agentCheckFail(SERVICE_INSTANCE_ID_PREFIX + serviceInstanceId);
    }

    public List<Url> find(String serviceName) {
        return find(serviceName, null, null);
    }

    public List<Url> findActive(String serviceName) {
        return find(serviceName, null, true);
    }

    public List<Url> find(String serviceName, String path) {
        return find(serviceName, path, null);
    }

    public List<Url> find(String serviceName, String path, Boolean active) {
        HealthServicesRequest.Builder queryBuilder = HealthServicesRequest.newBuilder()
                .setQueryParams(new QueryParams(CONSUL_QUERY_TIMEOUT_SECONDS, 0));
        if (StringUtils.isNotEmpty(path)) {
            queryBuilder.setTag(TAG_PREFIX_PATH + SEMICOLON + path);
        }
        if (active != null) {
            queryBuilder.setPassing(active);
        }
        Response<List<HealthService>> response = consulClient.getHealthServices(serviceName, queryBuilder.build());
        if (response == null) {
            return Collections.emptyList();
        }
        List<Url> providerUrls = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(response.getValue())) {
            for (HealthService serviceInstance : response.getValue()) {
                Optional.ofNullable(ConsulUtils.buildUrl(serviceInstance)).ifPresent(providerUrls::add);
            }
        }
        return providerUrls;
    }
}

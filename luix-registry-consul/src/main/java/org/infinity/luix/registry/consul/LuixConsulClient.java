package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.kv.model.GetValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.registry.consul.utils.ConsulUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LuixConsulClient {
    /**
     * Consul query timeout in seconds
     */
    public static        long         CONSUL_QUERY_TIMEOUT_SECONDS   = TimeUnit.MINUTES.toSeconds(9);
    /**
     * Command key prefix
     */
    public static final  String       CONSUL_LUIX_COMMAND_KEY_PREFIX = "luix/command/";
    /**
     * Service instance ID prefix
     */
    private static final String       SERVICE_INSTANCE_ID_PREFIX     = "service:";
    /**
     * Consul client instance
     */
    public static        ConsulClient consulClient;

    public LuixConsulClient(String host, int port) {
        consulClient = new ConsulClient(host, port);
        log.info("Initialized consul client with host: [{}] and port: [{}]", host, port);
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

    public ConsulResponse<List<ConsulService>> queryActiveServices(String serviceName, long lastConsulIndex) {
        ConsulResponse<List<ConsulService>> consulResponse = new ConsulResponse<>();
        QueryParams queryParams = new QueryParams(CONSUL_QUERY_TIMEOUT_SECONDS, lastConsulIndex);
        Response<List<HealthService>> response = consulClient.getHealthServices(serviceName, true, queryParams);
        if (response != null && CollectionUtils.isNotEmpty(response.getValue())) {
            List<ConsulService> activeServiceInstances = new ArrayList<>(response.getValue().size());
            for (HealthService activeServiceInstance : response.getValue()) {
                try {
                    activeServiceInstances.add(ConsulService.of(activeServiceInstance));
                } catch (Exception e) {
                    String serviceInstanceId = activeServiceInstance.getService() != null
                            ? activeServiceInstance.getService().getId()
                            : null;
                    log.error("Failed to convert to consul service with ID: [" + serviceInstanceId + "]", e);
                }
            }
            if (CollectionUtils.isNotEmpty(activeServiceInstances)) {
                consulResponse.setValue(activeServiceInstances);
                consulResponse.setConsulIndex(response.getConsulIndex());
                consulResponse.setConsulLastContact(response.getConsulLastContact());
                consulResponse.setConsulKnownLeader(response.isConsulKnownLeader());
            }
        }
        return consulResponse;
    }

    public String queryCommand(String form) {
        String key = CONSUL_LUIX_COMMAND_KEY_PREFIX + ConsulUtils.buildServiceName(form);
        GetValue value = consulClient.getKVValue(key).getValue();
        String command = StringUtils.EMPTY;
        if (value == null) {
            log.warn("No command found with form: [{}]", form);
        } else if (value.getValue() != null) {
            command = value.getDecodedValue();
        }
        return command;
    }
}

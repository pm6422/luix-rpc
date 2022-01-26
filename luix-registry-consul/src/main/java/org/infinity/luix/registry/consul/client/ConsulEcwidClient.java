package org.infinity.luix.registry.consul.client;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.kv.model.GetValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.registry.consul.ConsulResponse;
import org.infinity.luix.registry.consul.ConsulService;
import org.infinity.luix.registry.consul.utils.ConsulUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ConsulEcwidClient extends AbstractConsulClient {
    /**
     * consul block查询时block的最长时间,单位(秒)
     */
    public static       long         CONSUL_BLOCK_TIME_SECONDS = TimeUnit.MINUTES.toSeconds(9);
    /**
     * motan rpc 在consul中存储command的目录
     */
    public static final String       CONSUL_MOTAN_COMMAND      = "motan/command/";
    public static       ConsulClient consulClient;

    public ConsulEcwidClient(String host, int port) {
        super(host, port);
        consulClient = new ConsulClient(host, port);
        log.info("Initialized consul client with host: [{}] and port: [{}]", host, port);
    }

    @Override
    public void checkPass(String serviceId) {
        consulClient.agentCheckPass("service:" + serviceId);
    }

    @Override
    public void checkFail(String serviceId) {
        consulClient.agentCheckFail("service:" + serviceId);
    }

    @Override
    public void registerService(ConsulService service) {
        NewService newService = service.toNewService();
        consulClient.agentServiceRegister(newService);
    }

    @Override
    public void deregisterService(String serviceId) {
        consulClient.agentServiceDeregister(serviceId);
    }

    @Override
    public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, long lastConsulIndex) {
        ConsulResponse<List<ConsulService>> consulResponse = new ConsulResponse<>();
        QueryParams queryParams = new QueryParams(CONSUL_BLOCK_TIME_SECONDS, lastConsulIndex);
        Response<List<HealthService>> consulHealthResults = consulClient.getHealthServices(serviceName, true, queryParams);
        if (consulHealthResults != null && CollectionUtils.isNotEmpty(consulHealthResults.getValue())) {
            List<HealthService> healthServices = consulHealthResults.getValue();
            List<ConsulService> consulServices = new ArrayList<>(healthServices.size());

            for (HealthService healthService : healthServices) {
                try {
                    consulServices.add(ConsulService.of(healthService));
                } catch (Exception e) {
                    String serviceId = healthService.getService() != null ? healthService.getService().getId() : null;
                    log.error("Failed to convert consul service with ID: [" + serviceId + "]", e);
                }
            }

            if (CollectionUtils.isNotEmpty(consulServices)) {
                consulResponse.setValue(consulServices);
                consulResponse.setConsulIndex(consulHealthResults.getConsulIndex());
                consulResponse.setConsulLastContact(consulHealthResults.getConsulLastContact());
                consulResponse.setConsulKnownLeader(consulHealthResults.isConsulKnownLeader());
            }
        }
        return consulResponse;
    }

    @Override
    public String lookupCommand(String group) {
        String key = CONSUL_MOTAN_COMMAND + ConsulUtils.buildServiceFormName(group);
        GetValue value = consulClient.getKVValue(key).getValue();
        String command = StringUtils.EMPTY;
        if (value == null) {
            log.warn("No command found with group: [{}]", group);
        } else if (value.getValue() != null) {
            command = value.getDecodedValue();
        }
        return command;
    }
}

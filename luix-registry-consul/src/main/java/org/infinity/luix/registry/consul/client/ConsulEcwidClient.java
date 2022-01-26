package org.infinity.luix.registry.consul.client;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.kv.model.GetValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
    public static       ConsulClient client;

    public ConsulEcwidClient(String host, int port) {
        super(host, port);
        client = new ConsulClient(host, port);
        log.info("Initialized consul client with host: [{}] and port: [{}]", host, port);
    }

    @Override
    public void checkPass(String serviceId) {
        client.agentCheckPass("service:" + serviceId);
    }

    @Override
    public void checkFail(String serviceId) {
        client.agentCheckFail("service:" + serviceId);
    }

    @Override
    public void registerService(ConsulService service) {
        NewService newService = service.toNewService();
        client.agentServiceRegister(newService);
    }

    @Override
    public void unregisterService(String serviceId) {
        client.agentServiceDeregister(serviceId);
    }

    @Override
    public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, long lastConsulIndex) {
        ConsulResponse<List<ConsulService>> consulResponse = null;
        QueryParams queryParams = new QueryParams(CONSUL_BLOCK_TIME_SECONDS, lastConsulIndex);
        Response<List<HealthService>> consulHealthResults = client.getHealthServices(serviceName, true, queryParams);
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
                consulResponse = new ConsulResponse<>();
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
        Response<GetValue> response = client.getKVValue(CONSUL_MOTAN_COMMAND + ConsulUtils.buildServiceFormName(group));
        GetValue value = response.getValue();
        String command = "";
        if (value == null) {
            log.info("no command in group: " + group);
        } else if (value.getValue() != null) {
            command = value.getDecodedValue();
        }
        return command;
    }
}

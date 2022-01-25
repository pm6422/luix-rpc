package org.infinity.luix.registry.consul.client;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.health.model.HealthService.Service;
import com.ecwid.consul.v1.kv.model.GetValue;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.registry.consul.ConsulResponse;
import org.infinity.luix.registry.consul.ConsulService;
import org.infinity.luix.registry.consul.utils.ConsulUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConsulEcwidClient extends AbstractConsulClient {

    /**
     * consul block 查询时 block的最长时间,单位，分钟
     */
    public static int CONSUL_BLOCK_TIME_MINUTES = 9;

    /**
     * consul block 查询时 block的最长时间,单位，秒
     */
    public static       long   CONSUL_BLOCK_TIME_SECONDS = CONSUL_BLOCK_TIME_MINUTES * 60;
    /**
     * motan rpc 在consul中存储command的目录
     */
    public static final String CONSUL_MOTAN_COMMAND      = "motan/command/";

    public static ConsulClient client;

    public ConsulEcwidClient(String host, int port) {
        super(host, port);
        client = new ConsulClient(host, port);
        log.info("ConsulEcwidClient init finish. client host:" + host + ", port:" + port);
    }

    @Override
    public void checkPass(String serviceid) {
        client.agentCheckPass("service:" + serviceid);
    }

    @Override
    public void registerService(ConsulService service) {
        NewService newService = service.toNewService();
        client.agentServiceRegister(newService);
    }

    @Override
    public void unregisterService(String serviceid) {
        client.agentServiceDeregister(serviceid);
    }

    @Override
    public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, long lastConsulIndex) {
        QueryParams queryParams = new QueryParams(CONSUL_BLOCK_TIME_SECONDS, lastConsulIndex);
        Response<List<HealthService>> orgResponse = client.getHealthServices(serviceName, true, queryParams);
        ConsulResponse<List<ConsulService>> newResponse = null;
        if (orgResponse != null && orgResponse.getValue() != null
                && !orgResponse.getValue().isEmpty()) {
            List<HealthService> healthServices = orgResponse.getValue();
            List<ConsulService> consulServices = new ArrayList<>(healthServices.size());

            for (HealthService healthService : healthServices) {
                try {
                    ConsulService newService = ConsulService.of(healthService);
                    consulServices.add(newService);
                } catch (Exception e) {
                    String servcieid = "null";
                    if (healthService.getService() != null) {
                        servcieid = healthService.getService().getId();
                    }
                    log.error("convert consul service fail. org consulservice:" + servcieid, e);
                }
            }
            if (!consulServices.isEmpty()) {
                newResponse = new ConsulResponse<>();
                newResponse.setValue(consulServices);
                newResponse.setConsulIndex(orgResponse.getConsulIndex());
                newResponse.setConsulLastContact(orgResponse
                        .getConsulLastContact());
                newResponse.setConsulKnownLeader(orgResponse
                        .isConsulKnownLeader());
            }
        }

        return newResponse;
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

    @Override
    public void checkFail(String serviceid) {
        client.agentCheckFail("service:" + serviceid);
    }
}

package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import lombok.Data;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.UrlUtils;
import org.infinity.luix.registry.consul.utils.ConsulUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConsulService {
    /**
     * Time to live for consul service, unit is second.
     * Each service will register a TTL type check. If the heartbeat is not sent within the maximum TTL seconds,
     * the service will become unavailable.
     */
    public static       int          TTL                 = 30;
    /**
     * Tag prefix for RPC protocol.
     */
    public static final String       TAG_PREFIX_PROTOCOL = "protocol_";
    /**
     * Tag prefix for RPC URL.
     */
    public static final String       TAG_PREFIX_URL      = "URL_";
    /**
     * Consul service name.
     */
    private             String       name;
    /**
     * Consul service instance name.
     */
    private             String       instanceName;
    /**
     * Consul server host.
     */
    private             String       address;
    /**
     * Consul server port.
     */
    private             Integer      port;
    /**
     * Consul service tags.
     */
    private             List<String> tags;

    public NewService toNewService() {
        NewService newService = new NewService();
        // Consul service name
        newService.setName(name);
        // Consul service instance name
        newService.setId(instanceName);
        // Consul server host
        newService.setAddress(address);
        // Consul server port
        newService.setPort(port);

        NewService.Check check = new NewService.Check();
        check.setTtl(TTL + "s");
        newService.setCheck(check);
        newService.setTags(tags);
        return newService;
    }

    public static ConsulService of(HealthService healthService) {
        ConsulService consulService = new ConsulService();
        HealthService.Service consulHealthService = healthService.getService();
        consulService.setAddress(consulHealthService.getAddress());
        consulService.setInstanceName(consulHealthService.getId());
        consulService.setName(consulHealthService.getService());
        consulService.setPort(consulHealthService.getPort());
        consulService.setTags(consulHealthService.getTags());
        return consulService;
    }

    public static ConsulService of(Url url) {
        ConsulService service = new ConsulService();
        service.setInstanceName(ConsulUtils.buildServiceInstanceId(url));
        service.setName(ConsulUtils.buildServiceName(url.getForm()));
        service.setAddress(url.getHost());
        service.setPort(url.getPort());
        service.setTags(buildTags(url));
        return service;
    }

    private static List<String> buildTags(Url url) {
        List<String> tags = new ArrayList<>();
        tags.add(TAG_PREFIX_PROTOCOL + url.getProtocol());
        tags.add(TAG_PREFIX_URL + UrlUtils.urlEncode(url.toFullStr()));
        return tags;
    }
}

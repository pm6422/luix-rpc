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
     * service 最长存活周期（Time To Live），单位秒。 每个service会注册一个ttl类型的check，在最长TTL秒不发送心跳
     * 就会将service变为不可用状态。
     */
    public static       int          TTL                 = 30;
    /**
     * motan协议在consul tag中的前缀
     */
    public static final String       CONSUL_TAG_PROTOCOL = "protocol_";
    public static final String       CONSUL_TAG_URL      = "URL_";
    private             String       id;
    private             String       name;
    private             String       address;
    private             Integer      port;
    private             long         ttl;
    private             List<String> tags;

    public NewService toNewService() {
        NewService newService = new NewService();
        newService.setId(id);
        newService.setName(name);
        newService.setAddress(address);
        newService.setPort(port);

        NewService.Check check = new NewService.Check();
        check.setTtl(ttl + "s");
        newService.setCheck(check);
        newService.setTags(tags);
        return newService;
    }

    public static ConsulService of(HealthService healthService) {
        ConsulService service = new ConsulService();
        HealthService.Service consulHealthService = healthService.getService();
        service.setAddress(consulHealthService.getAddress());
        service.setId(consulHealthService.getId());
        service.setName(consulHealthService.getService());
        service.setPort(consulHealthService.getPort());
        service.setTags(consulHealthService.getTags());
        return service;
    }

    public static ConsulService of(Url url) {
        ConsulService service = new ConsulService();
        service.setId(ConsulUtils.buildServiceId(url));
        service.setName(ConsulUtils.buildServiceFormName(url.getForm()));
        service.setAddress(url.getHost());
        service.setPort(url.getPort());
        service.setTtl(TTL);
        service.setTags(buildTags(url));
        return service;
    }

    private static List<String> buildTags(Url url) {
        List<String> tags = new ArrayList<>();
        tags.add(CONSUL_TAG_PROTOCOL + url.getProtocol());
        tags.add(CONSUL_TAG_URL + UrlUtils.urlEncode(url.toFullStr()));
        return tags;
    }
}

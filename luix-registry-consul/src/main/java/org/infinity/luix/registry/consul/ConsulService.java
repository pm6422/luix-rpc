package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import lombok.Data;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.UrlUtils;
import org.infinity.luix.registry.consul.utils.ConsulUtils;

import java.util.ArrayList;
import java.util.List;

import static org.infinity.luix.registry.consul.utils.ConsulUtils.*;

@Data
public class ConsulService {
    /**
     * Time to live for consul service, unit is second.
     * Each consul service instance will be registered a TTL type check.
     * If the heartbeat is not sent within the maximum TTL seconds, the service will become 'failing'.
     */
    public static       int          TTL             = 30;
    /**
     * Tag prefix for RPC URL.
     */
    public static final String       TAG_PREFIX_URL  = "URL";
    /**
     * Tag prefix for RPC path.
     */
    public static final String       TAG_PREFIX_PATH = "PATH";
    /**
     * Consul service name.
     */
    private             String       name;
    /**
     * Consul service instance ID.
     */
    private             String       instanceId;
    /**
     * RPC service host.
     */
    private             String       address;
    /**
     * RPC service port.
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
        // Consul service instance ID
        newService.setId(instanceId);
        // RPC service host
        newService.setAddress(address);
        // RPC service port
        newService.setPort(port);

        NewService.Check check = new NewService.Check();
        check.setTtl(TTL + "s");
        newService.setCheck(check);
        newService.setTags(tags);
        return newService;
    }

    public static ConsulService byUrl(Url url) {
        ConsulService consulService = new ConsulService();
        if (url.isProvider()) {
            consulService.setName(CONSUL_PROVIDING_SERVICE_NAME);
        } else {
            consulService.setName(CONSUL_CONSUMING_SERVICE_NAME);
        }
        consulService.setInstanceId(ConsulUtils.buildInstanceId(url));
        consulService.setAddress(url.getHost());
        consulService.setPort(url.getPort());
        consulService.setTags(buildTags(url));
        return consulService;
    }

    private static List<String> buildTags(Url url) {
        List<String> tags = new ArrayList<>(2);
        tags.add(TAG_PREFIX_URL + SEMICOLON + UrlUtils.urlEncode(url.toFullStr()));
        tags.add(TAG_PREFIX_PATH + SEMICOLON + url.getPath());
        return tags;
    }
}

package org.infinity.luix.registry.consul.utils;

import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.UrlUtils;
import org.infinity.luix.registry.consul.ConsulService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.infinity.luix.core.constant.RpcConstants.NODE_TYPE_SERVICE;
import static org.infinity.luix.registry.consul.ConsulService.CONSUL_TAG_PROTOCOL;
import static org.infinity.luix.registry.consul.ConsulService.CONSUL_TAG_URL;

public class ConsulUtils {

    /**
     * Service form prefix name on consul registry
     */
    private static final String CONSUL_SERVICE_FORM_PREFIX = "luix";
    private static final String DELIMITER                  = "-";

    /**
     * 根据url生成consul的service ID。 service id包括ip＋port＋rpc服务的接口类名
     *
     * @param url
     * @return
     */
    public static String buildServiceId(Url url) {
        return url == null ? null : url.getHost() + ":" + url.getPort() + DELIMITER + url.getPath();
    }

    public static String buildServiceFormName(String form) {
        return isNotEmpty(form) ? CONSUL_SERVICE_FORM_PREFIX + DELIMITER + form : CONSUL_SERVICE_FORM_PREFIX;
    }

    public static String extractFromName(String serviceName) {
        return CONSUL_SERVICE_FORM_PREFIX.equals(serviceName)
                ? StringUtils.EMPTY
                : serviceName.substring(CONSUL_SERVICE_FORM_PREFIX.length() + 1);
    }

    /**
     * 判断两个list中的url是否一致。 如果任意一个list为空，则返回false； 此方法并未做严格互相判等
     *
     * @param urls1
     * @param urls2
     * @return
     */
    public static boolean isSame(List<Url> urls1, List<Url> urls2) {
        if (urls1 == null || urls2 == null || urls1.size() != urls2.size()) {
            return false;
        }
        return urls1.containsAll(urls2);
    }

    /**
     * 根据service生成motan使用的
     *
     * @param service
     * @return
     */
    public static Url buildUrl(ConsulService service) {
        Url url = null;
        for (String tag : service.getTags()) {
            if (tag.startsWith(CONSUL_TAG_URL)) {
                String encodeUrl = tag.substring(tag.indexOf("_") + 1);
                url = Url.valueOf(UrlUtils.urlDecode(encodeUrl));
            }
        }

        if (url == null) {
            Map<String, String> params = new HashMap<>(2);
            params.put(Url.PARAM_FROM, extractFromName(service.getName()));
            params.put(Url.PARAM_TYPE, NODE_TYPE_SERVICE);

            String protocol = ConsulUtils.getProtocolFromTag(service.getTags().get(0));
            url = Url.of(protocol, service.getAddress(), service.getPort(),
                    ConsulUtils.getPathFromServiceId(service.getId()), params);
        }
        return url;
    }

    /**
     * 根据url获取cluster信息，cluster 信息包括协议和path(rpc服务中的接口类)
     *
     * @param url
     * @return
     */
    public static String getUrlClusterInfo(Url url) {
        return url.getProtocol() + DELIMITER + url.getPath();
    }

    /**
     * 从consul 的serviceId中获取rpc服务的接口类名（url的path）
     *
     * @param serviceId
     * @return
     */
    public static String getPathFromServiceId(String serviceId) {
        return serviceId.substring(serviceId.indexOf(DELIMITER) + 1);
    }

    /**
     * 从consul的tag获取protocol
     *
     * @param tag
     * @return
     */
    public static String getProtocolFromTag(String tag) {
        return tag.substring(CONSUL_TAG_PROTOCOL.length());
    }
}

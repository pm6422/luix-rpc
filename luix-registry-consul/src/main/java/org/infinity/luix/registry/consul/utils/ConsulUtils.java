package org.infinity.luix.registry.consul.utils;

import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.UrlUtils;
import org.infinity.luix.registry.consul.ConsulService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.infinity.luix.core.constant.RpcConstants.NODE_TYPE_SERVICE;
import static org.infinity.luix.registry.consul.ConsulService.TAG_PREFIX_PROTOCOL;
import static org.infinity.luix.registry.consul.ConsulService.TAG_PREFIX_URL;

public class ConsulUtils {

    /**
     * Active RPC provider service name on consul registry
     */
    private static final String CONSUL_PROVIDING_SERVICES_PREFIX  = "luix-providing";
    /**
     * Active RPC consumer service name on consul registry
     */
    private static final String CONSUL_CONSUMING_SERVICES_PREFIX  = "luix-consuming";
    /**
     *
     */
    private static final String CONSUL_SERVICE_INSTANCE_DELIMITER = "@";
    /**
     *
     */
    public static final  String CONSUL_TAG_DELIMITER              = ":";
    /**
     *
     */
    private static final String FORM_DELIMITER                    = ":";

    /**
     * Build consul service name for RPC provider
     *
     * @param form service provider form
     * @return consul service name
     */
    public static String buildServiceName(String form) {
        return isEmpty(form)
                ? CONSUL_PROVIDING_SERVICES_PREFIX
                : CONSUL_PROVIDING_SERVICES_PREFIX + FORM_DELIMITER + form;
    }

    /**
     * Build consul service instance name
     *
     * @param url url
     * @return consul service instance name
     */
    public static String buildServiceInstanceName(Url url) {
        return url == null
                ? null
                : url.getPath() + CONSUL_SERVICE_INSTANCE_DELIMITER + url.getHost() + ":" + url.getPort();
    }

    /**
     * Extract form name from service name string
     *
     * @param serviceName consul service name
     * @return form name
     */
    public static String getFormFromServiceName(String serviceName) {
        return CONSUL_PROVIDING_SERVICES_PREFIX.equals(serviceName)
                ? StringUtils.EMPTY
                : serviceName.substring(CONSUL_PROVIDING_SERVICES_PREFIX.length() + 1);
    }

    /**
     * Extract RPC service protocol from consul service tag
     *
     * @param consulServiceTag consul service tag
     * @return RPC service protocol
     */
    public static String getProtocolFromTag(String consulServiceTag) {
        return consulServiceTag.substring(TAG_PREFIX_PROTOCOL.length());
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
            if (tag.startsWith(TAG_PREFIX_URL)) {
                String encodedUrl = tag.substring(tag.indexOf(CONSUL_TAG_DELIMITER) + 1);
                url = Url.valueOf(UrlUtils.urlDecode(encodedUrl));
            }
        }

        if (url == null) {
            Map<String, String> params = new HashMap<>(2);
            params.put(Url.PARAM_FROM, getFormFromServiceName(service.getName()));
            params.put(Url.PARAM_TYPE, NODE_TYPE_SERVICE);

            String protocol = ConsulUtils.getProtocolFromTag(service.getTags().get(0));
            url = Url.of(protocol, service.getAddress(), service.getPort(),
                    ConsulUtils.getPathFromServiceId(service.getInstanceId()), params);
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
        return url.getProtocol() + CONSUL_SERVICE_INSTANCE_DELIMITER + url.getPath();
    }

    /**
     * 从consul 的serviceId中获取rpc服务的接口类名（url的path）
     *
     * @param serviceId
     * @return
     */
    public static String getPathFromServiceId(String serviceId) {
        return serviceId.substring(serviceId.indexOf(CONSUL_SERVICE_INSTANCE_DELIMITER) + 1);
    }


}

package org.infinity.luix.registry.consul.utils;

import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.UrlUtils;
import org.infinity.luix.registry.consul.ConsulService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.infinity.luix.core.url.Url.PARAM_FROM;
import static org.infinity.luix.core.url.Url.PARAM_TYPE;
import static org.infinity.luix.registry.consul.ConsulService.TAG_PREFIX_PROTOCOL;
import static org.infinity.luix.registry.consul.ConsulService.TAG_PREFIX_URL;

public class ConsulUtils {

    /**
     * Active RPC provider service name on consul registry
     */
    public static final  String CONSUL_PROVIDING_SERVICES_PREFIX  = "luix-providing";
    /**
     * Active RPC consumer service name on consul registry
     */
    public static final  String CONSUL_CONSUMING_SERVICES_PREFIX  = "luix-consuming";
    /**
     *
     */
    public static final  String CONSUL_SERVICE_INSTANCE_DELIMITER = "@";
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
     * @return consul provider service name
     */
    public static String buildProviderServiceName(String form) {
        return isEmpty(form)
                ? CONSUL_PROVIDING_SERVICES_PREFIX
                : CONSUL_PROVIDING_SERVICES_PREFIX + FORM_DELIMITER + form;
    }

    /**
     * Build consul service name for RPC consumer
     *
     * @param form service consumer form
     * @return consul consumer service name
     */
    public static String buildConsumerServiceName(String form) {
        return isEmpty(form)
                ? CONSUL_CONSUMING_SERVICES_PREFIX
                : CONSUL_CONSUMING_SERVICES_PREFIX + FORM_DELIMITER + form;
    }

    /**
     * Build consul service instance ID
     *
     * @param url url
     * @return consul service instance ID
     */
    public static String buildServiceInstanceId(Url url) {
        StringBuilder sb = new StringBuilder(StringUtils.EMPTY);
        if (url != null) {
            sb.append(url.getPath()).append(CONSUL_SERVICE_INSTANCE_DELIMITER)
                    .append(url.getHost()).append(FORM_DELIMITER).append(url.getPort())
                    .append(CONSUL_SERVICE_INSTANCE_DELIMITER).append(url.getOption(PARAM_TYPE));

            if (StringUtils.isNotEmpty(url.getOption(PARAM_FROM))) {
                sb.append(CONSUL_SERVICE_INSTANCE_DELIMITER).append(url.getOption(PARAM_FROM));
            }
        }
        return sb.toString();
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
     * Get RPC protocol plus interface name string
     *
     * @param url url
     * @return RPC protocol plus interface name string
     */
    public static String getProtocolPlusPath(Url url) {
        return url.getProtocol() + FORM_DELIMITER + url.getPath();
    }

    /**
     * Get path from consul service instance ID
     *
     * @param serviceInstanceId consul service instance ID
     * @return path
     */
    public static String getPathFromServiceInstanceId(String serviceInstanceId) {
        return serviceInstanceId.substring(0, serviceInstanceId.indexOf(CONSUL_SERVICE_INSTANCE_DELIMITER));
    }

    /**
     * Build URL from consul service
     *
     * @param consulService consul service
     * @return URL
     */
    public static Url buildUrl(ConsulService consulService) {
        Url url = null;
        // Get URL from consul service tags
        for (String tag : consulService.getTags()) {
            if (tag.startsWith(TAG_PREFIX_URL)) {
                String encodedUrl = tag.substring(tag.indexOf(CONSUL_TAG_DELIMITER) + 1);
                url = Url.valueOf(UrlUtils.urlDecode(encodedUrl));
                break;
            }
        }

        if (url == null) {
            // Get URL from consul service instance ID
            Map<String, String> params = new HashMap<>(2);
            params.put(Url.PARAM_FROM, getFormFromServiceName(consulService.getName()));
            params.put(PARAM_TYPE, Url.PARAM_TYPE_PROVIDER);

            String protocol = ConsulUtils.getProtocolFromTag(consulService.getTags().get(0));
            url = Url.of(protocol, consulService.getAddress(), consulService.getPort(),
                    ConsulUtils.getPathFromServiceInstanceId(consulService.getInstanceId()), params);
        }
        return url;
    }

    /**
     * Determine whether two lists of URLs are consistent
     *
     * @param urls1 URL list 1
     * @param urls2 URL list 2
     * @return
     */
    public static boolean isSame(List<Url> urls1, List<Url> urls2) {
        if (urls1 == null || urls2 == null || urls1.size() != urls2.size()) {
            return false;
        }
        return urls1.containsAll(urls2);
    }
}

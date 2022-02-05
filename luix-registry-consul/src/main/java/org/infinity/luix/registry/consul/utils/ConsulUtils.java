package org.infinity.luix.registry.consul.utils;

import com.ecwid.consul.v1.health.model.HealthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.UrlUtils;

import static org.infinity.luix.core.constant.ServiceConstants.FORM;
import static org.infinity.luix.registry.consul.ConsulService.TAG_PREFIX_URL;

@Slf4j
public class ConsulUtils {

    /**
     * Active RPC provider service name on consul registry
     */
    public static final  String CONSUL_PROVIDING_SERVICE_NAME     = "luix-provider";
    /**
     * Active RPC consumer service name on consul registry
     */
    public static final  String CONSUL_CONSUMING_SERVICE_NAME     = "luix-consumer";
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
     * Build consul service instance ID
     *
     * @param url url
     * @return consul service instance ID
     */
    public static String buildInstanceId(Url url) {
        StringBuilder sb = new StringBuilder(StringUtils.EMPTY);
        if (url != null) {
            sb.append(url.getPath()).append(CONSUL_SERVICE_INSTANCE_DELIMITER)
                    .append(url.getHost()).append(FORM_DELIMITER).append(url.getPort())
                    .append(CONSUL_SERVICE_INSTANCE_DELIMITER).append(url.getType());

            if (StringUtils.isNotEmpty(url.getOption(FORM))) {
                sb.append(CONSUL_SERVICE_INSTANCE_DELIMITER).append(url.getOption(FORM));
            }
        }
        return sb.toString();
    }

    /**
     * Build URL from consul service
     *
     * @param healthService consul service
     * @return URL
     */
    public static Url buildUrl(HealthService healthService) {
        Url url = null;
        // Get URL from consul service tags
        for (String tag : healthService.getService().getTags()) {
            if (tag.startsWith(TAG_PREFIX_URL)) {
                String encodedUrl = tag.substring(tag.indexOf(CONSUL_TAG_DELIMITER) + 1);
                url = Url.valueOf(UrlUtils.urlDecode(encodedUrl));
                break;
            }
        }
        if (url == null) {
            log.warn("Can't build URL from consul service: {}", healthService);
        }
        return url;
    }
}

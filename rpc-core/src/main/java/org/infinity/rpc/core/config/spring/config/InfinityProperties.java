package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.registry.Protocol;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.regex.Pattern;

/**
 * - Application
 * - TransferProtocol
 * - Registry
 */
@ConfigurationProperties(prefix = "infinity")
@Data
public class
InfinityProperties implements InitializingBean {
    public static final Pattern           COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    private             Application       application;
    private             TransportProtocol transportProtocol;
    private             Registry          registry;

    @Override
    public void afterPropertiesSet() throws Exception {
        Validate.notNull(application, "Application must NOT the null, please check your configuration!");
        Validate.notNull(transportProtocol, "Transport protocol must NOT the null, please check your configuration!");
        Validate.notNull(registry, "Registry must NOT the null, please check your configuration!");
        initialize();
    }

    private void initialize() {
        registry.initialize();
    }

    @Data
    public static class Application {
        // Application ID
        private String id;
        // Application name
        private String name;
        // Application description
        private String description;
        // Environment variable, e.g. dev, test or prod
        private String env;
    }

    @Data
    public static class TransportProtocol {
        // RPC server port
        private Integer port;
    }

    @Data
    public static class Registry {
        // Protocol for register center
        // SpringBoot properties binding mechanism can automatically convert the string value in config file to enum type, and check whether value is valid during application startup.
        private Protocol protocol;
        // Registry center server address
        private String   address;
        // Registry center host name
        private String   host;
        // Registry center port number
        private Integer  port;
        // 注册中心连接超时时间(毫秒)
        private Integer  connectTimeout;
        // 注册中心会话超时时间(毫秒)
//        private             Integer  sessionTimeout  = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
        private Integer  sessionTimeout;
        // 注册中心请求超时时间(毫秒)
        private Integer  requestTimeout;
        // 注册中心连接失败后重试的时间间隔(毫秒)
        private Integer  retryInterval;

        public void initialize() {
            checkIntegrity();
            checkValidity();
            if (StringUtils.isNotEmpty(address) && StringUtils.isEmpty(host) && port == null) {
                String[] splitParts = COLON_SPLIT_PATTERN.split(address);
                host = splitParts[0];
                port = Integer.parseInt(splitParts[1]);
            }
            if (StringUtils.isEmpty(address) && StringUtils.isNotEmpty(host) && port != null) {
                address = host + ":" + port;
            }
        }

        private void checkIntegrity() {
            // todo
        }

        private void checkValidity() {
            // todo
        }
    }
}

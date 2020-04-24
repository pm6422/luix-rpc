package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.registry.Protocol;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.regex.Pattern;

/**
 * - Registry
 * - TransferProtocol
 */
@ConfigurationProperties(prefix = "spring.infinity-rpc")
@Data
public class InfinityRpcProperties implements InitializingBean {
    public static final Pattern           COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    private             Registry          registry;
    private             TransportProtocol transportProtocol;

    @Override
    public void afterPropertiesSet() throws Exception {
        checkIntegrity();
        checkValidity();
        assignValues();
    }

    private void checkIntegrity() {
        // todo
    }

    private void checkValidity() {
        // todo
    }

    private void assignValues() {
        if (StringUtils.isNotEmpty(registry.getAddress()) && StringUtils.isEmpty(registry.getHost()) && registry.getPort() == null) {
            String[] splitParts = COLON_SPLIT_PATTERN.split(registry.getAddress());
            registry.setHost(splitParts[0]);
            registry.setPort(Integer.parseInt(splitParts[1]));
        }
        if (StringUtils.isEmpty(registry.getAddress()) && StringUtils.isNotEmpty(registry.getHost()) && registry.getPort() != null) {
            registry.setAddress(registry.getHost() + ":" + registry.getPort());
        }
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
        // 注册中心请求超时时间(毫秒)
        private Integer  requestTimeout;
        // 注册中心连接超时时间(毫秒)
        private Integer  connectTimeout;
        // 注册中心会话超时时间(毫秒)
//        private             Integer  sessionTimeout  = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
        private Integer  sessionTimeout;
    }

    @Data
    public static class TransportProtocol {
        // RPC server port
        private Integer port;
    }
}

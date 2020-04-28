package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.exception.InfinityConfigurationException;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.utilities.id.ShortIdWorker;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
    public static final Pattern     COLON_SPLIT_PATTERN = Pattern.compile("\\s*[:]+\\s*");
    private             Application application         = new Application();
    private             Protocol    protocol            = new Protocol();
    private             Registry    registry            = new Registry();

    @Override
    public void afterPropertiesSet() throws Exception {
        Validate.notNull(application, "Application must NOT the null, please check your configuration!");
        Validate.notNull(registry, "Registry must NOT the null, please check your configuration!");
        initialize();
    }

    private void initialize() {
        protocol.initialize();
        registry.initialize();
    }

    @Data
    public static class Application {
        // Application ID
        private String id = "ID-" + new ShortIdWorker().nextId();
        // Application name
        private String name;
        // Application description
        private String description;
        // Group used to discover services
        private String group;
        // Environment variable, e.g. dev, test or prod
        private String env;
    }

    @Data
    public static class Protocol {
        public enum Name {
            INFINITY("infinity");

            private String value;

            Name(String value) {
                this.value = value;
            }

            public Registry.Name fromValue(String value) {
                return Registry.Name.valueOf(value);
            }

            public String value() {
                return value;
            }

            @Override
            public String toString() {
                return value;
            }
        }

        // Name of protocol
        // SpringBoot properties binding mechanism can automatically convert the string value in config file to enum type, and check whether value is valid during application startup.
        private Name    name = Name.INFINITY;
        // Port number of the RPC server
        private Integer port;

        public void initialize() {
            checkIntegrity();
            checkValidity();
        }

        private void checkIntegrity() {
            // todo

        }

        private void checkValidity() {
        }
    }

    @Data
    public static class Registry {
        public enum Name {
            ZOOKEEPER("zookeeper");

            private String value;

            Name(String value) {
                this.value = value;
            }

            public Name fromValue(String value) {
                return Name.valueOf(value);
            }

            public String value() {
                return value;
            }

            @Override
            public String toString() {
                return value;
            }
        }

        // Type name of register center
        private Name    name           = Name.ZOOKEEPER;
        // Registry center host name
        private String  host;
        // Registry center port number
        private Integer port;
        // Registry center server address
        private String  address;
        // 注册中心连接超时时间(毫秒)
        private Integer connectTimeout = Math.toIntExact(TimeUnit.SECONDS.toMillis(1));
        // 注册中心会话超时时间(毫秒)
        private Integer sessionTimeout = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
        // 注册中心连接失败后重试的时间间隔(毫秒)
        private Integer retryInterval  = Math.toIntExact(TimeUnit.SECONDS.toMillis(30));
        // 注册中心请求超时时间(毫秒)
        private Integer requestTimeout;

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
            Optional.ofNullable(ServiceInstanceLoader.getServiceLoader(RegistryFactory.class).load(name.value))
                    .orElseThrow(() -> new InfinityConfigurationException("Failed to load the proper registry factory, " +
                            "please check whether the dependency [rpc-registry-" + name.value + "] is in your class path!"));
        }
    }
}

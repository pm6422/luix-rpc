package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.exception.InfinityConfigurationException;
import org.infinity.rpc.core.protocol.constants.ProtocolName;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.constants.RegistryName;
import org.infinity.rpc.utilities.id.ShortIdWorker;
import org.infinity.rpc.utilities.network.NetworkIpUtils;
import org.springframework.beans.BeanUtils;
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
public class InfinityProperties implements InitializingBean {
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
        application.initialize();
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
        // Responsible team
        private String team;
        // Application owner
        private String owner;
        // Environment variable, e.g. dev, test or prod
        private String env;

        public void initialize() {
            checkIntegrity();
            checkValidity();
        }

        private void checkIntegrity() {
            Validate.notNull(name, "Application name must NOT be null! Please check your configuration.");
        }

        private void checkValidity() {
        }

        public App toApp() {
            App app = new App();
            BeanUtils.copyProperties(this, app);
            return app;
        }
    }

    @Data
    public static class Protocol {
        // Name of protocol
        // SpringBoot properties binding mechanism can automatically convert the string value in config file to enum type,
        // and check whether value is valid or not during application startup.
        private ProtocolName name = ProtocolName.infinity;
        // Host name of the RPC server
        private String       host = NetworkIpUtils.INTRANET_IP;
        // Port number of the RPC server
        private Integer      port;

        public void initialize() {
            checkIntegrity();
            checkValidity();
        }

        private void checkIntegrity() {
            Validate.notNull(port, "Protocol port must NOT be null! Please check your configuration.");
        }

        private void checkValidity() {
        }
    }

    @Data
    public static class Registry {
        // Name of register center
        private RegistryName name           = RegistryName.zookeeper;
        // Registry center host name
        private String       host;
        // Registry center port number
        private Integer      port;
        // Registry center server address
        private String       address;
        // 注册中心连接超时时间(毫秒)
        private Integer      connectTimeout = Math.toIntExact(TimeUnit.SECONDS.toMillis(1));
        // 注册中心会话超时时间(毫秒)
        private Integer      sessionTimeout = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
        // 注册中心连接失败后重试的时间间隔(毫秒)
        private Integer      retryInterval  = Math.toIntExact(TimeUnit.SECONDS.toMillis(30));
        // 注册中心请求超时时间(毫秒)
        private Integer      requestTimeout;

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
            Optional.ofNullable(RegistryFactory.getInstance(name.getValue()))
                    .orElseThrow(() -> new InfinityConfigurationException("Failed to load the proper registry factory, " +
                            "please check whether the dependency [rpc-registry-" + name.getValue() + "] is in your class path!"));
        }
    }
}

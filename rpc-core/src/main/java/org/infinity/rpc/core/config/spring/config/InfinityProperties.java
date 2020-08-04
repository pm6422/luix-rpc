package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.protocol.constants.ProtocolName;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.constants.RegistryName;
import org.infinity.rpc.utilities.id.IdGenerator;
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
    private             ApplicationConfig application         = new ApplicationConfig();
    // TODO: support multiple protocols
    private             ProtocolConfig    protocol            = new ProtocolConfig();
    // TODO: support multiple registries
    private             RegistryConfig    registry            = new RegistryConfig();

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
}

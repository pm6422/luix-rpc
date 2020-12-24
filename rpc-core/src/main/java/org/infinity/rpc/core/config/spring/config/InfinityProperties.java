package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

/**
 * - Application
 * - TransferProtocol
 * - Registry
 */
@ConfigurationProperties(prefix = "infinity")
@Data
@Validated
public class InfinityProperties implements InitializingBean {
    @NotNull
    private ApplicationConfig application;
    /**
     * TODO: support multiple protocols
     */
    private ProtocolConfig    protocol    = new ProtocolConfig();
    /**
     * TODO: support multiple registries
     */
    @NotNull
    private RegistryConfig    registry;

    @Override
    public void afterPropertiesSet() {
        init();
    }

    private void init() {
        application.initialize();
        protocol.initialize();
        registry.initialize();
    }
}

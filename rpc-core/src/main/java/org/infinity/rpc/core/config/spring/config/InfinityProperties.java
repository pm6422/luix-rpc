package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

/**
 * - Application
 * - Transport protocol
 * - Registry
 */
@ConfigurationProperties(prefix = "infinity")
@Data
@Validated
public class InfinityProperties {
    @NotNull
    private ApplicationConfig application;
    /**
     * TODO: support multiple protocols
     */
    @NotNull
    private ProtocolConfig    protocol = new ProtocolConfig();
    /**
     * TODO: support multiple registries
     */
    @NotNull
    private RegistryConfig    registry;

    @PostConstruct
    private void init() {
        application.init();
        protocol.init();
        registry.init();
    }
}

package org.infinity.rpc.spring.boot.config;

import lombok.Data;
import org.infinity.rpc.core.config.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

/**
 * - Application
 * - Transport protocol
 * - Registry
 * - Global provider config
 * - Global consumer config
 */
@ConfigurationProperties(prefix = "infinity")
@Data
@Validated
public class InfinityProperties {
    @NotNull
    private ApplicationConfig application = new ApplicationConfig();
    /**
     * TODO: support multiple protocols
     */
    @NotNull
    private ProtocolConfig    protocol    = new ProtocolConfig();
    /**
     * TODO: support multiple registries
     */
    @NotNull
    private RegistryConfig    registry    = new RegistryConfig();
    /**
     *
     */
    @NotNull
    private ProviderConfig    provider    = new ProviderConfig();
    /**
     *
     */
    @NotNull
    private ConsumerConfig    consumer    = new ConsumerConfig();

    @PostConstruct
    private void init() {
        application.init();
        protocol.init();
        registry.init();
        provider.init();
        consumer.init();
    }
}

package org.infinity.rpc.spring.boot.config;

import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.config.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private ApplicationConfig           application = new ApplicationConfig();
    /**
     *
     */
    private ProtocolConfig              protocol    = new ProtocolConfig();
    /**
     *
     */
    private Map<String, ProtocolConfig> protocols   = new LinkedHashMap<>(5);
    /**
     *
     */
    private RegistryConfig              registry    = new RegistryConfig();
    /**
     *
     */
    private Map<String, RegistryConfig> registries  = new LinkedHashMap<>(5);
    /**
     *
     */
    @NotNull
    private ProviderConfig              provider    = new ProviderConfig();
    /**
     *
     */
    @NotNull
    private ConsumerConfig              consumer    = new ConsumerConfig();

    @PostConstruct
    private void init() {
        application.init();
        getProtocolList().forEach(ProtocolConfig::init);
        getRegistryList().forEach(RegistryConfig::init);
        provider.init();
        consumer.init();
    }

    public Collection<ProtocolConfig> getProtocolList() {
        if (MapUtils.isEmpty(protocols)) {
            return Collections.singletonList(protocol);
        }
        return protocols.values();
    }

    public ProtocolConfig getAvailableProtocol() {
        // todo: support multiple protocols
        // 当配置多个protocol的时候，比如A,B,C，
        // 那么正常情况下只会使用A，如果A被开关降级，那么就会使用B，B也被降级，那么会使用C
        return protocols.size() > 0 ? protocols.get(0) : protocol;
    }

    public Collection<RegistryConfig> getRegistryList() {
        if (MapUtils.isEmpty(registries)) {
            return Collections.singletonList(registry);
        }
        return registries.values();
    }
}

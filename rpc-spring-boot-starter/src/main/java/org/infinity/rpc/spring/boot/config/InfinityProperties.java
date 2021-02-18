package org.infinity.rpc.spring.boot.config;

import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.config.*;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;

import static org.infinity.rpc.core.constant.RegistryConstants.REGISTRY_VALUE_DIRECT;

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
    private RegistryConfig              registry    = new RegistryConfig();
    /**
     * Supports multiple registries
     */
    private Map<String, RegistryConfig> registries  = new LinkedHashMap<>(5);
    /**
     *
     */
    private ProtocolConfig              protocol    = new ProtocolConfig();
    /**
     * Supports multiple protocols
     */
    private Map<String, ProtocolConfig> protocols   = new LinkedHashMap<>(5);
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
        checkValidity();
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
        if (registries.size() > 1) {
            Optional<RegistryConfig> registryConfig = registries.values()
                    .stream()
                    .filter(registry -> registry.getName().equals(REGISTRY_VALUE_DIRECT))
                    .findAny();
            if (registryConfig.isPresent()) {
                throw new RpcConfigurationException("Do NOT use direct registry when using multiple registries!");
            }
        }
        return registries.values();
    }

    private void checkValidity() {
    }
}

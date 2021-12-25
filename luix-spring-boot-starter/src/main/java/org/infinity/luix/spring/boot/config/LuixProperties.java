package org.infinity.luix.spring.boot.config;

import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.infinity.luix.core.config.impl.*;
import org.infinity.luix.core.exception.impl.RpcConfigException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.SpringVersion;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.*;

import static org.infinity.luix.core.constant.RegistryConstants.REGISTRY_VAL_NONE;
import static org.infinity.luix.spring.boot.config.LuixProperties.PREFIX;
import static org.infinity.luix.spring.boot.utils.PropertySourcesUtils.readProperties;

/**
 * - Application
 * - Transport protocol
 * - Registry
 * - Global provider config
 * - Global consumer config
 */
@ConfigurationProperties(prefix = PREFIX)
@Data
@Validated
public class LuixProperties implements InitializingBean {
    public static final String                      PREFIX      = "luix";
    @NotNull
    private             ApplicationConfig           application = new ApplicationConfig();
    /**
     *
     */
    private             RegistryConfig              registry    = new RegistryConfig();
    /**
     * Supports multiple registries
     */
    private             Map<String, RegistryConfig> registries  = new LinkedHashMap<>(5);
    /**
     *
     */
    private             ProtocolConfig              protocol    = new ProtocolConfig();
    /**
     * Supports multiple protocols
     */
    private             Map<String, ProtocolConfig> protocols   = new LinkedHashMap<>(5);
    /**
     *
     */
    @NotNull
    private             ProviderConfig              provider    = new ProviderConfig();
    /**
     *
     */
    @NotNull
    private             ConsumerConfig              consumer    = new ConsumerConfig();

    @Override
    public void afterPropertiesSet() {
        application.init();
        application.setSpringBootVersion(SpringBootVersion.getVersion());
        application.setSpringVersion(SpringVersion.getVersion());
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
        return protocols.size() > 0 ? new ArrayList<>(protocols.values()).get(0) : protocol;
    }

    public Collection<RegistryConfig> getRegistryList() {
        if (MapUtils.isEmpty(registries)) {
            return Collections.singletonList(registry);
        }
        if (registries.size() > 1) {
            Optional<RegistryConfig> registryConfig = registries.values()
                    .stream()
                    .filter(registry -> registry.getName().equals(REGISTRY_VAL_NONE))
                    .findAny();
            if (registryConfig.isPresent()) {
                throw new RpcConfigException("Do NOT use direct registry when using multiple registries!");
            }
        }
        return registries.values();
    }

    private void checkValidity() {
    }

    public static ProtocolConfig readProtocolConfig(ConfigurableEnvironment env) {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        readProperties(env.getPropertySources(), env, PREFIX.concat(".").concat(ProtocolConfig.PREFIX), ProtocolConfig.class, protocolConfig);
        return protocolConfig;
    }

    public static ProviderConfig readProviderConfig(ConfigurableEnvironment env) {
        ProviderConfig providerConfig = new ProviderConfig();
        readProperties(env.getPropertySources(), env, PREFIX.concat(".").concat(ProviderConfig.PREFIX), ProviderConfig.class, providerConfig);
        return providerConfig;
    }
}

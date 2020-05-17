package org.infinity.rpc.webcenter.config;

import lombok.Data;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "infinity.registry", ignoreUnknownFields = false)
@Data
public class InfinityRegistryProperties extends InfinityProperties.Registry {
}

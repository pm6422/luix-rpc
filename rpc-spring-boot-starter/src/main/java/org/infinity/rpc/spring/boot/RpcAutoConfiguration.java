package org.infinity.rpc.spring.boot;

import org.infinity.rpc.core.config.*;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @EnableConfigurationProperties({InfinityProperties.class}) can NOT load application-test.yml properly,
 * It only can load properties of application.yml.
 * So we have to load all yml files programmatically.
 */
//@EnableInfinityPropertiesBindings({
//        @InfinityConfigurationProperties(prefix = "infinity.application", type = ApplicationConfig.class),
//        @InfinityConfigurationProperties(prefix = "infinity.protocol", type = ProtocolConfig.class),
//        @InfinityConfigurationProperties(prefix = "infinity.registry", type = RegistryConfig.class),
//        @InfinityConfigurationProperties(prefix = "infinity.provider", type = ProviderConfig.class),
//        @InfinityConfigurationProperties(prefix = "infinity.consumer", type = ConsumerConfig.class)
//})
@EnableConfigurationProperties({InfinityProperties.class})
public class RpcAutoConfiguration {
}

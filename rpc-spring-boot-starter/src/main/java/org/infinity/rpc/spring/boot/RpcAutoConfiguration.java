package org.infinity.rpc.spring.boot;

import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({InfinityProperties.class})
public class RpcAutoConfiguration {
}

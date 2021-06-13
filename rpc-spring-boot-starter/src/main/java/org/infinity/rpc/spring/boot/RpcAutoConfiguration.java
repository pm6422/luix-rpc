package org.infinity.rpc.spring.boot;

import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.buildin.impl.BuildInServiceImpl;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties({InfinityProperties.class})
public class RpcAutoConfiguration {

    @Bean
    public BuildInService buildInService() {
        return new BuildInServiceImpl();
    }
}

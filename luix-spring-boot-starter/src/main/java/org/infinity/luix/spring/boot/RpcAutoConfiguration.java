package org.infinity.luix.spring.boot;

import org.infinity.luix.core.server.buildin.BuildInService;
import org.infinity.luix.core.server.buildin.impl.BuildInServiceImpl;
import org.infinity.luix.spring.boot.config.LuixProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties({LuixProperties.class})
public class RpcAutoConfiguration {

    @Bean
    public BuildInService buildInService() {
        return new BuildInServiceImpl();
    }
}

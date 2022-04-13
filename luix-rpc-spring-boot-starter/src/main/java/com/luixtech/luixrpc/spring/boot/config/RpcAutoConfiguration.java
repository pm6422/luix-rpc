package com.luixtech.luixrpc.spring.boot.config;

import com.luixtech.luixrpc.core.server.buildin.BuildInService;
import com.luixtech.luixrpc.core.server.buildin.impl.BuildInServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties({LuixProperties.class})
public class RpcAutoConfiguration {

    @Bean
    public BuildInService buildInService() {
        return new BuildInServiceImpl();
    }
}

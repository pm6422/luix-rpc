package com.luixtech.rpc.spring.boot.starter.config;

import com.luixtech.rpc.core.server.buildin.BuildInService;
import com.luixtech.rpc.core.server.buildin.impl.BuildInServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties({LuixProperties.class})
public class RpcAutoConfiguration {

    @Bean
    public BuildInService buildInService() {
        return new BuildInServiceImpl();
    }
}

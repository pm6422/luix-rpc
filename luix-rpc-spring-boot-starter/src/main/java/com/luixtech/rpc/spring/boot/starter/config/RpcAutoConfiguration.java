package com.luixtech.rpc.spring.boot.starter.config;

import com.luixtech.rpc.core.server.buildin.BuildInService;
import com.luixtech.rpc.core.server.buildin.impl.BuildInServiceImpl;
import com.luixtech.rpc.spring.boot.starter.endpoint.LuixApiEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@EnableConfigurationProperties({LuixRpcProperties.class})
public class RpcAutoConfiguration {

    @Bean
    public BuildInService buildInService() {
        return new BuildInServiceImpl();
    }

    /**
     * <p>OpenApiEndpoint</p>
     *
     * @return a {@link LuixApiEndpoint} object.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @ConditionalOnExpression("${luix.application.enable-endpoint:true} || ${luix.application.enableEndpoint:true}")
    public LuixApiEndpoint luixApiEndpoint() {
        return new LuixApiEndpoint();
    }
}

package org.infinity.rpc.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableConfigurationProperties({InfinityProperties.class})
@Configuration
public class RpcClientAutoConfiguration {

    @Autowired
    private InfinityProperties infinityProperties;

    @Autowired
    private ApplicationContext applicationContext;


//    @Bean
//    public RpcConsumerProxy rpcConsumerProxy(ZkRpcServerRegistry rpcServiceRegistry) {
//        return new RpcConsumerProxy(rpcServiceRegistry);
//    }
}

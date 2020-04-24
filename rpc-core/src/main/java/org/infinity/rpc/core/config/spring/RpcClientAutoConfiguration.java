package org.infinity.rpc.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityRpcProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableConfigurationProperties({InfinityRpcProperties.class})
@Configuration
public class RpcClientAutoConfiguration {

    @Autowired
    private InfinityRpcProperties infinityRpcProperties;

    @Autowired
    private ApplicationContext applicationContext;

//    @Bean
//    public ZkRpcServerRegistry rpcServiceRegistry() {
//        return new ZkRpcServerRegistry(infinityRpcProperties.getRegistry().getAddress());
//    }

//    @Bean
//    public RpcConsumerProxy rpcConsumerProxy(ZkRpcServerRegistry rpcServiceRegistry) {
//        return new RpcConsumerProxy(rpcServiceRegistry);
//    }
//
//    @Bean
//    public RpcServer rpcServer() {
//        return new RpcServer(infinityRpcProperties.getProtocol().getPort(), rpcServiceRegistry());
//    }
}

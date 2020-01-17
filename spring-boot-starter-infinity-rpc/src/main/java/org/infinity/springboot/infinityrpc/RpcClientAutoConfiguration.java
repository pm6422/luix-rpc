package org.infinity.springboot.infinityrpc;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.RpcConsumerProxy;
import org.infinity.rpc.registry.ZkRpcServerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableConfigurationProperties({InfinityRpcProperties.class})
@Configuration
public class RpcClientAutoConfiguration {

    @Autowired
    private InfinityRpcProperties infinityRpcProperties;

    @Bean
    public ZkRpcServerRegistry rpcServiceRegistry() {
        ZkRpcServerRegistry rpcServiceRegistry = new ZkRpcServerRegistry();
        rpcServiceRegistry.setRegistryAddress(infinityRpcProperties.getRegistry().getAddress());
        return rpcServiceRegistry;
    }

    @Bean
    public RpcConsumerProxy rpcConsumerProxy(ZkRpcServerRegistry rpcServiceRegistry) {
        return new RpcConsumerProxy(rpcServiceRegistry);
    }
}

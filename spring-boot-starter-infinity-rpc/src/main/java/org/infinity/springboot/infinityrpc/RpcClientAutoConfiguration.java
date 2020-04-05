package org.infinity.springboot.infinityrpc;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.infinity.rpc.registry.ZkRpcServerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RpcClientAutoConfiguration {

    @Autowired
    private ZkRpcServerRegistry rpcServiceRegistry;

    @Bean
    public RpcConsumerProxy rpcConsumerProxy(ZkRpcServerRegistry rpcServiceRegistry) {
        return new RpcConsumerProxy(rpcServiceRegistry);
    }
}

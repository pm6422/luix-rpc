package org.infinity.rpc.appserver.config;

import org.infinity.rpc.registry.ZkRpcServerRegistry;
import org.infinity.rpc.server.RpcServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RpcConfiguration {
    @Bean
    public ZkRpcServerRegistry rpcServiceRegistry() {
        ZkRpcServerRegistry rpcServiceRegistry = new ZkRpcServerRegistry();
        rpcServiceRegistry.setRegistryAddress("127.0.0.1:2181");
        return rpcServiceRegistry;
    }

    @Bean
    public RpcServer rpcServer(ZkRpcServerRegistry rpcServiceRegistry) {
        return new RpcServer("127.0.0.1:6010", rpcServiceRegistry);
    }
}

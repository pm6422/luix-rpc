package org.infinity.rpc.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.registry.ZkRegistryRpcServerDiscovery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableConfigurationProperties({RpcClientProperties.class})
@Configuration
public class RpcClientConfiguration {

    @Autowired
    private RpcClientProperties rpcClientProperties;

    @Bean
    public ZkRegistryRpcServerDiscovery rpcServerDiscovery() {
        ZkRegistryRpcServerDiscovery zkRegistryRpcServerDiscovery = new ZkRegistryRpcServerDiscovery(rpcClientProperties.getRegistry().getAddress());
        return zkRegistryRpcServerDiscovery;
    }
}

package org.infinity.rpc.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.registry.ZookeeperRpcServerDiscovery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//import org.infinity.rpc.client.annotation.Consumer;

@Slf4j
@EnableConfigurationProperties({RpcClientProperties.class})
@Configuration
public class RpcClientConfiguration {

    @Autowired
    private RpcClientProperties rpcClientProperties;

    @Bean
    public ZookeeperRpcServerDiscovery rpcServerDiscovery() throws Exception {
        return new ZookeeperRpcServerDiscovery(rpcClientProperties.getRegistry().getAddress());
    }

    @Bean
    public RpcClientProxy rpcClientProxy(ZookeeperRpcServerDiscovery rpcServerDiscovery) throws Exception {
        return new RpcClientProxy(rpcServerDiscovery);
    }
}

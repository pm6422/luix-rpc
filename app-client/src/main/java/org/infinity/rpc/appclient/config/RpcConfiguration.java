package org.infinity.rpc.appclient.config;

import org.infinity.rpc.client.RpcClientProxy;
import org.infinity.rpc.registry.ZookeeperRpcServerDiscovery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RpcConfiguration {

    @Value("${application.registry.address}")
    private String registryAddress;

    @Bean
    public ZookeeperRpcServerDiscovery rpcDiscover() throws Exception {
        return new ZookeeperRpcServerDiscovery(registryAddress);
    }

    @Bean
    public RpcClientProxy rpcClientProxy(ZookeeperRpcServerDiscovery zookeeperRpcServerDiscovery) {
        return new RpcClientProxy(zookeeperRpcServerDiscovery);
    }
}

package org.infinity.rpc.appclient.config;

import org.infinity.rpc.client.RpcClientProxy;
import org.infinity.rpc.registry.RpcZookeeperServerDiscovery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RpcConfiguration {

    @Value("${application.registry.address}")
    private String registryAddress;

    @Bean
    public RpcZookeeperServerDiscovery rpcDiscover() throws Exception {
        return new RpcZookeeperServerDiscovery(registryAddress);
    }

    @Bean
    public RpcClientProxy rpcClientProxy(RpcZookeeperServerDiscovery rpcZookeeperServerDiscovery) {
        return new RpcClientProxy(rpcZookeeperServerDiscovery);
    }
}

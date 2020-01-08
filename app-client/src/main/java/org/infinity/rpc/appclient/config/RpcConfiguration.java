package org.infinity.rpc.appclient.config;

import org.infinity.rpc.client.RpcClientProxy;
import org.infinity.rpc.registry.ZookeeperServerDiscovery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RpcConfiguration {

    @Value("${application.registry.address}")
    private String registryAddress;

    @Bean
    public ZookeeperServerDiscovery rpcDiscover() throws Exception {
        return new ZookeeperServerDiscovery(registryAddress);
    }

    @Bean
    public RpcClientProxy rpcClientProxy(ZookeeperServerDiscovery zookeeperServerDiscovery) {
        return new RpcClientProxy(zookeeperServerDiscovery);
    }
}

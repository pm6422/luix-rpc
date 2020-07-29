package org.infinity.rpc.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.config.spring.startup.NettyServerApplicationRunner;
import org.infinity.rpc.core.registry.Registrable;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.Url;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@EnableConfigurationProperties({InfinityProperties.class})
public class RpcAutoConfiguration {
    @Autowired
    private InfinityProperties infinityProperties;

    @Bean
    public RpcConsumerProxy rpcConsumerProxy() {
        return new RpcConsumerProxy(infinityProperties);
    }

    @Bean
    public ApplicationRunner nettyServerApplicationRunner() {
        return new NettyServerApplicationRunner();
    }
}

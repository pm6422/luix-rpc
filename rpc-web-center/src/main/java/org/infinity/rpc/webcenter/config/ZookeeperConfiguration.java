package org.infinity.rpc.webcenter.config;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.infinity.rpc.webcenter.service.impl.ZookeeperRegistryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;

@Configuration
@ConditionalOnProperty(prefix = "infinity.registry", value = "name", havingValue = "zookeeper")
@Slf4j
public class ZookeeperConfiguration {

    @Autowired
    private InfinityRegistryProperties infinityRegistryProperties;

    @Bean
    public ZkClient zkClient() {
        try {
            return new ZkClient(infinityRegistryProperties.getAddress(), infinityRegistryProperties.getSessionTimeout(), infinityRegistryProperties.getConnectTimeout());
        } catch (ZkException e) {
            log.error(MessageFormat.format("Failed to connect zookeeper server with error: {0}", e.getMessage()), e);
            return null;
        }
    }

    @Bean
    public RegistryService registryService() {
        return new ZookeeperRegistryServiceImpl(zkClient());
    }
}

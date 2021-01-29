package org.infinity.rpc.webcenter.config;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.infinity.rpc.webcenter.service.impl.ZookeeperRegistryServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "infinity.registry", value = "name", havingValue = "zookeeper")
@Slf4j
public class ZookeeperRegistryConfiguration {

    private final InfinityProperties infinityProperties;

    public ZookeeperRegistryConfiguration(InfinityProperties infinityProperties) {
        this.infinityProperties = infinityProperties;
    }

    @Bean
    public List<RegistryService> registryServices() {
        try {
            List<RegistryService> registryServices = new ArrayList<>(infinityProperties.getRegistryList().size());
            infinityProperties.getRegistryList().forEach(registryConfig -> {
                Url registryUrl = registryConfig.getRegistryUrl();
                int connectTimeout = registryUrl.getIntParameter(Url.PARAM_CONNECT_TIMEOUT);
                int sessionTimeout = registryUrl.getIntParameter(Url.PARAM_CONNECT_TIMEOUT);
                ZkClient zkClient = createZkClient(registryUrl.getParameter(Url.PARAM_ADDRESS), sessionTimeout, connectTimeout);
                registryServices.add(new ZookeeperRegistryServiceImpl(zkClient));
            });
            return registryServices;
        } catch (ZkException e) {
            throw new RpcFrameworkException("Failed to connect zookeeper server with error", e);
        }
    }

    private ZkClient createZkClient(String zkServers, int sessionTimeout, int connectionTimeout) {
        return new ZkClient(zkServers, sessionTimeout, connectionTimeout);
    }
}

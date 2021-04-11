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

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static org.infinity.rpc.core.constant.RegistryConstants.CONNECT_TIMEOUT;
import static org.infinity.rpc.core.constant.RegistryConstants.SESSION_TIMEOUT;

@Configuration
@ConditionalOnProperty(prefix = "infinity.registry", value = "name", havingValue = "zookeeper")
@Slf4j
public class ZookeeperRegistryConfiguration {

    @Resource
    private InfinityProperties infinityProperties;

    @Bean
    public List<RegistryService> registryServices() {
        try {
            List<RegistryService> registryServices = new ArrayList<>(infinityProperties.getRegistryList().size());
            infinityProperties.getRegistryList().forEach(registryConfig -> {
                Url registryUrl = registryConfig.getRegistryUrl();
                int connectTimeout = registryUrl.getIntOption(CONNECT_TIMEOUT);
                int sessionTimeout = registryUrl.getIntOption(SESSION_TIMEOUT);
                ZkClient zkClient = createZkClient(registryUrl.getAddress(), sessionTimeout, connectTimeout);
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

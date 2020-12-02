package org.infinity.rpc.webcenter.config;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.infinity.rpc.webcenter.service.impl.ZookeeperRegistryServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "infinity.registry", value = "name", havingValue = "zookeeper")
@Slf4j
public class ZookeeperRegistryConfiguration {

    private final RegistryInfo registryInfo;

    public ZookeeperRegistryConfiguration(RegistryInfo registryInfo) {
        this.registryInfo = registryInfo;
    }

    @Bean
    public RegistryService registryService() {
        try {
            // TODO: Support multiple registry centers
            Url registryUrl = registryInfo.getRegistryUrls().get(0);
            int connectTimeout = registryUrl.getIntParameter(Url.PARAM_CONNECT_TIMEOUT);
            int sessionTimeout = registryUrl.getIntParameter(Url.PARAM_CONNECT_TIMEOUT);
            ZkClient zkClient = createZkClient(registryUrl.getParameter(Url.PARAM_ADDRESS), sessionTimeout, connectTimeout);
            return new ZookeeperRegistryServiceImpl(zkClient);
        } catch (ZkException e) {
            throw new RpcFrameworkException("Failed to connect zookeeper server with error", e);
        }
    }

    private ZkClient createZkClient(String zkServers, int sessionTimeout, int connectionTimeout) {
        return new ZkClient(zkServers, sessionTimeout, connectionTimeout);
    }
}

package org.infinity.rpc.webcenter.config;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.registry.zookeeper.ZookeeperRegistry;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.infinity.rpc.webcenter.service.impl.ZookeeperRegistryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;
import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "infinity.registry", value = "name", havingValue = "zookeeper")
@Slf4j
public class ZookeeperRegistryConfiguration {

    @Autowired
    private List<Url> registryUrls;

    @Bean
    public RegistryService registryService() {
        try {
            // TODO: Support multiple registry centers
            Url registryUrl = registryUrls.get(0);
            int connectTimeout = registryUrl.getIntParameter(Url.PARAM_CONNECT_TIMEOUT);
            int sessionTimeout = registryUrl.getIntParameter(Url.PARAM_CONNECT_TIMEOUT);
            ZkClient zkClient = createZkClient(registryUrl.getParameter(Url.PARAM_ADDRESS), sessionTimeout, connectTimeout);
            return new ZookeeperRegistryServiceImpl(zkClient);
        } catch (ZkException e) {
            log.error(MessageFormat.format("Failed to connect zookeeper server with error: {0}", e.getMessage()), e);
            return null;
        }
    }

    private ZkClient createZkClient(String zkServers, int sessionTimeout, int connectionTimeout) {
        return new ZkClient(zkServers, sessionTimeout, connectionTimeout);
    }
}

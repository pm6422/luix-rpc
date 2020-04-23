package org.infinity.rpc.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.infinity.rpc.core.registry.*;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

@ServiceName("zookeeper")
@Slf4j
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry createRegistry(Url registryUrl) {
        try {
            int timeout = registryUrl.getIntParameter(UrlParam.connectTimeout.getName(), UrlParam.connectTimeout.getIntValue());
            int sessionTimeout = registryUrl.getIntParameter(UrlParam.registrySessionTimeout.getName(), UrlParam.registrySessionTimeout.getIntValue());
            ZkClient zkClient = createZkClient(registryUrl.getParameter("address"), sessionTimeout, timeout);
            return new ZookeeperRegistry(registryUrl, zkClient);
        } catch (ZkException e) {
            log.error("Failed to connect zookeeper server with error: {}", e.getMessage());
            throw e;
        }
    }

    private ZkClient createZkClient(String zkServers, int sessionTimeout, int connectionTimeout) {
        return new ZkClient(zkServers, sessionTimeout, connectionTimeout);
    }
}

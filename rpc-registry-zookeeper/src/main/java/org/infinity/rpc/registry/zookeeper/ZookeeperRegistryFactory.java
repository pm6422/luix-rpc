package org.infinity.rpc.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.registry.AbstractRegistryFactory;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.RegistryConstants.*;

@SpiName(REGISTRY_VAL_ZOOKEEPER)
@Slf4j
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    /**
     * Create a zookeeper registry
     *
     * @param registryUrl registry URL
     * @return registry instance
     */
    @Override
    public Registry createRegistry(Url registryUrl) {
        int sessionTimeout = registryUrl.getIntOption(SESSION_TIMEOUT, SESSION_TIMEOUT_VAL_DEFAULT);
        int connectTimeout = registryUrl.getIntOption(CONNECT_TIMEOUT, CONNECT_TIMEOUT_VAL_DEFAULT);
        ZkClient zkClient = createZkClient(registryUrl.getAddress(), sessionTimeout, connectTimeout);
        return new ZookeeperRegistry(zkClient, registryUrl);
    }

    private ZkClient createZkClient(String zkServers, int sessionTimeout, int connectionTimeout) {
        return new ZkClient(zkServers, sessionTimeout, connectionTimeout);
    }
}

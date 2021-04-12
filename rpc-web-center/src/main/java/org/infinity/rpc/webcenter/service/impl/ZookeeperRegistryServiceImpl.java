package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.webcenter.service.RegistryService;

import java.util.List;
import java.util.Map;

import static org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils.FULL_PATH_PROVIDER;

@Slf4j
public class ZookeeperRegistryServiceImpl implements RegistryService {

    private final ZkClient zkClient;

    public ZookeeperRegistryServiceImpl(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    @Deprecated
    @Override
    public List<String> getGroups() {
        return ZookeeperUtils.getChildrenNames(zkClient, FULL_PATH_PROVIDER);
    }

    @Deprecated
    @Override
    public List<AddressInfo> getNodes(String group, String provider, String statusNode) {
//        return ZookeeperUtils.getProviderAddresses(zkClient, group, provider, ZookeeperStatusNode.fromValue(statusNode));
        return null;
    }

    @Deprecated
    @Override
    public Map<String, Map<String, List<AddressInfo>>> getAllNodes(String group) {
        return null;
//        return ZookeeperUtils.getAllProviders(zkClient, group);
    }
}

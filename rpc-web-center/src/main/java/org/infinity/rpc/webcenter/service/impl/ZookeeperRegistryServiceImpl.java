package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.registry.zookeeper.ZookeeperStatusNode;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.webcenter.service.RegistryService;

import java.util.List;
import java.util.Map;

@Slf4j
public class ZookeeperRegistryServiceImpl implements RegistryService {

    private final ZkClient zkClient;

    public ZookeeperRegistryServiceImpl(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    @Deprecated
    @Override
    public List<String> getGroups() {
        return ZookeeperUtils.getAllProviderFroms(zkClient);
    }

    @Deprecated
    @Override
    public List<String> getProvidersByGroup(String group) {
        return ZookeeperUtils.getProvidersByForm(zkClient, group);
    }

    @Deprecated
    @Override
    public List<AddressInfo> getNodes(String group, String provider, String statusNode) {
        return ZookeeperUtils.getProviderAddresses(zkClient, group, provider, ZookeeperStatusNode.fromValue(statusNode));
    }

    @Deprecated
    @Override
    public Map<String, Map<String, List<AddressInfo>>> getAllNodes(String group) {
        return ZookeeperUtils.getAllProviders(zkClient, group);
    }

    @Deprecated
    @Override
    public List<ApplicationExtConfig> getAllApplications() {
        return ZookeeperUtils.getAllAppInfo(zkClient);
    }
}

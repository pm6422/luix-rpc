package org.infinity.rpc.webcenter.service.impl;

import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.registry.zookeeper.utils.AddressInfo;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.webcenter.service.RegistryService;

import java.util.List;
import java.util.Map;

public class ZookeeperRegistryServiceImpl implements RegistryService {

    private ZkClient zkClient;

    public ZookeeperRegistryServiceImpl(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    @Override
    public List<String> getGroups() {
        return ZookeeperUtils.getGroups(zkClient);
    }

    @Override
    public List<String> getProvidersByGroup(String group) {
        return ZookeeperUtils.getProvidersByGroup(zkClient, group);
    }

    @Override
    public List<AddressInfo> getNodes(String group, String provider, String statusNode) {
        return ZookeeperUtils.getNodes(zkClient, group, provider, statusNode);
    }

    @Override
    public Map<String, Map<String, List<AddressInfo>>> getAllNodes(String group) {
        return ZookeeperUtils.getAllNodes(zkClient, group);
    }

    @Override
    public List<String> getAllApplications() {
        return ZookeeperUtils.getAllApplications(zkClient);
    }
}

package org.infinity.rpc.webcenter.service.impl;

import org.I0Itec.zkclient.ZkClient;
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
    public List<Map<String, String>> getNodes(String group, String service, String statusNode) {
        return ZookeeperUtils.getNodes(zkClient, group, service, statusNode);
    }

    @Override
    public List<Map<String, List<Map<String, String>>>> getAllNodes(String group) {
        return ZookeeperUtils.getAllNodes(zkClient, group);
    }
}

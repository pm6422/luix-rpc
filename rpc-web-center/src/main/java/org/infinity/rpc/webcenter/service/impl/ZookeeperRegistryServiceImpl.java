package org.infinity.rpc.webcenter.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.registry.zookeeper.utils.AddressInfo;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.webcenter.service.RegistryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ZookeeperRegistryServiceImpl implements RegistryService {

    private final ZkClient zkClient;

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
    public List<ApplicationExtConfig> getAllApplications() {
        List<ApplicationExtConfig> apps = new ArrayList<>();
        List<String> appNames = ZookeeperUtils.getAllApplications(zkClient);
        if (CollectionUtils.isEmpty(appNames)) {
            return apps;
        }
        try {
            for (String appName : appNames) {
                String applicationInfoPath = ZookeeperUtils.getApplicationInfoPath(appName);
                String info = zkClient.readData(applicationInfoPath, true);
                ApplicationExtConfig app = new ApplicationExtConfig();
                if (info != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    app = objectMapper.readValue(info, ApplicationExtConfig.class);
                } else {
                    app.setName(appName);
                }
                apps.add(app);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to read application info from zookeeper", e);
        }
        return apps;
    }
}

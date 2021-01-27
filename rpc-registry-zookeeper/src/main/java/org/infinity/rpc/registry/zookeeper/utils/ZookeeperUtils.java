package org.infinity.rpc.registry.zookeeper.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.registry.zookeeper.ZookeeperStatusNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.core.url.Url.PATH_SEPARATOR;

@Slf4j
public class ZookeeperUtils {

    public static final String REGISTRY_NAMESPACE = "/infinity";
    public static final String REGISTRY_COMMAND   = "/command";
    public static final String APP_DIR            = "/app";
    public static final String APP_FILE_NAME      = "info";
    public static final String APP_PATH           = REGISTRY_NAMESPACE + APP_DIR;
    public static final String APP_PROVIDER_DIR   = "/app-provider";
    public static final String PROVIDER_DIR       = "/provider";
    public static final String PROVIDER_PATH      = REGISTRY_NAMESPACE + PROVIDER_DIR;

    /**
     * Get all app names
     *
     * @param zkClient zk client
     * @return app names
     */
    public static List<String> getAllAppNames(ZkClient zkClient) {
        return getChildNodeNames(zkClient, APP_PATH);
    }

    /**
     * Get app info file path
     *
     * @param appName application name
     * @return app info file path
     */
    public static String getAppInfoFilePath(String appName) {
        return getAppPath(appName) + PATH_SEPARATOR + APP_FILE_NAME;
    }

    /**
     * Get the full path of specified application node
     *
     * @param appName application name
     * @return specified application node full path
     */
    public static String getAppPath(String appName) {
        return APP_PATH + PATH_SEPARATOR + appName;
    }

    /**
     * Read all app info from file
     *
     * @param zkClient zk client
     * @return app info
     */
    public static List<ApplicationExtConfig> getAllAppInfo(ZkClient zkClient) {
        List<ApplicationExtConfig> apps = new ArrayList<>();
        List<String> appNames = getAllAppNames(zkClient);
        if (CollectionUtils.isEmpty(appNames)) {
            return apps;
        }
        try {
            for (String appName : appNames) {
                String appInfoPath = getAppInfoFilePath(appName);
                String info = zkClient.readData(appInfoPath, true);
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
            log.error("Failed to read application info from zookeeper!", e);
        }
        return apps;
    }

    /**
     * Get all the provider groups
     *
     * @param zkClient zk client
     * @return provider groups
     */
    public static List<String> getAllProviderGroups(ZkClient zkClient) {
        return getChildNodeNames(zkClient, PROVIDER_PATH);
    }

    public static Map<String, Map<String, List<AddressInfo>>> getAllProviders(ZkClient zkClient, String group) {
        List<String> providerNames = getProvidersByGroup(zkClient, group);
        Map<String, Map<String, List<AddressInfo>>> results = new HashMap<>(providerNames.size());
        for (String providerName : providerNames) {
            Map<String, List<AddressInfo>> providersPerStatus = new HashMap<>();
            List<AddressInfo> activeProviders = getNodes(zkClient, group, providerName, ZookeeperStatusNode.ACTIVE);
            providersPerStatus.put(ZookeeperStatusNode.ACTIVE.getValue(), activeProviders);
            List<AddressInfo> inactiveProviders = getNodes(zkClient, group, providerName, ZookeeperStatusNode.INACTIVE);
            providersPerStatus.put(ZookeeperStatusNode.INACTIVE.getValue(), inactiveProviders);
            List<AddressInfo> clientProviders = getNodes(zkClient, group, providerName, ZookeeperStatusNode.CLIENT);
            providersPerStatus.put(ZookeeperStatusNode.CLIENT.getValue(), clientProviders);

            results.put(providerName, providersPerStatus);
        }
        return results;
    }

    /**
     * Get providers by group
     *
     * @param zkClient zk client
     * @param group    group
     * @return provider names
     */
    public static List<String> getProvidersByGroup(ZkClient zkClient, String group) {
        return getChildNodeNames(zkClient, getGroupPath(group));
    }

    /**
     * Get the full path of provider group node
     *
     * @param group group
     * @return group node full path
     */
    public static String getGroupPath(String group) {
        return PROVIDER_PATH + PATH_SEPARATOR + group;
    }

    public static List<AddressInfo> getNodes(ZkClient zkClient, String group, String providerPath, ZookeeperStatusNode statusNode) {
        List<AddressInfo> result = new ArrayList<>();
        List<String> nodes = getChildNodeNames(zkClient, getStatusNodePath(group, providerPath, statusNode));
        for (String nodeName : nodes) {
            AddressInfo addressInfo = new AddressInfo();
            String info = zkClient.readData(getAddressPath(group, providerPath, statusNode, nodeName), true);
            addressInfo.setAddress(nodeName);
            addressInfo.setContents(info);
            result.add(addressInfo);
        }
        return result;
    }

    /**
     * Get the provider address full path of specified node
     *
     * @param url  url
     * @param node zookeeper active status node
     * @return address full path
     */
    public static String getAddressPath(Url url, ZookeeperStatusNode node) {
        return getStatusNodePath(url, node) + PATH_SEPARATOR + url.getServerPortStr();
    }

    /**
     * Get the provider status node full path under specified group and status node
     *
     * @param url  url
     * @param node zookeeper active status node
     * @return provider status node full path
     */
    public static String getStatusNodePath(Url url, ZookeeperStatusNode node) {
        return getStatusNodePath(url.getGroup(), url.getPath(), node);
    }

    /**
     * Get the provider status node full path under specified group and status node
     *
     * @param group        zookeeper group node
     * @param providerPath provider class fully-qualified name
     * @param node         status node
     * @return provider status node full path
     */
    public static String getStatusNodePath(String group, String providerPath, ZookeeperStatusNode node) {
        return getProviderPath(group, providerPath) + PATH_SEPARATOR + node.getValue();
    }

    /**
     * Get the provider address node full path under specified group and status node
     *
     * @param group        zookeeper group node
     * @param providerPath provider class fully-qualified name
     * @param node         status node
     * @param address      provider address
     * @return provider status node full path
     */
    public static String getAddressPath(String group, String providerPath, ZookeeperStatusNode node, String address) {
        return getStatusNodePath(group, providerPath, node) + PATH_SEPARATOR + address;
    }

    /**
     * Get the provider full path under specified group node
     *
     * @param url url
     * @return provider full path
     */
    public static String getProviderPath(Url url) {
        return getProviderPath(url.getGroup(), url.getPath());
    }

    /**
     * Get the provider full path under specified group node
     *
     * @param group        group
     * @param providerPath provider class fully-qualified name
     * @return provider full path
     */
    public static String getProviderPath(String group, String providerPath) {
        return getGroupPath(group) + PATH_SEPARATOR + providerPath;
    }

    /**
     * Get the command full path
     *
     * @param url url
     * @return command full path
     */
    public static String getCommandPath(Url url) {
        return getGroupPath(url) + REGISTRY_COMMAND;
    }

    /**
     * Get the group node full path
     *
     * @param url url
     * @return group node full path
     */
    public static String getGroupPath(Url url) {
        return getGroupPath(url.getGroup());
    }


    /**
     * Get all child directory or file names under the parent path
     *
     * @param path zookeeper directory path
     * @return child node names
     */
    public static List<String> getChildNodeNames(ZkClient zkClient, String path) {
        List<String> children = new ArrayList<>();
        if (zkClient.exists(path)) {
            children = zkClient.getChildren(path);
        }
        return children;
    }

    public static List<AddressInfo> getNodes(ZkClient zkClient, String group, String providerPath, String statusNode) {
        return getNodes(zkClient, group, providerPath, ZookeeperStatusNode.fromValue(statusNode));
    }

    /**
     * Get the full path of specified application provider
     *
     * @param appName application name
     * @return specified application provider node full path
     */
    public static String getAppProviderPath(String appName) {
        return REGISTRY_NAMESPACE + APP_PROVIDER_DIR + PATH_SEPARATOR + appName;
    }
}

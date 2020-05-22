package org.infinity.rpc.registry.zookeeper.utils;

import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.registry.zookeeper.ZookeeperStatusNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.core.registry.Url.PATH_SEPARATOR;

public class ZookeeperUtils {

    public static final String ZOOKEEPER_REGISTRY_NAMESPACE = "/infinity";


    public static final String ZOOKEEPER_REGISTRY_COMMAND = "/command";

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
        return getGroupPath(url) + ZOOKEEPER_REGISTRY_COMMAND;
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
     * Get the full path of group node
     *
     * @param group group
     * @return group node full path
     */
    public static String getGroupPath(String group) {
        return ZOOKEEPER_REGISTRY_NAMESPACE + PATH_SEPARATOR + group;
    }

    /**
     * Get all child nodes under the path
     *
     * @param path zookeeper directory path
     * @return child nodes
     */
    public static List<String> getChildren(ZkClient zkClient, String path) {
        List<String> children = new ArrayList<>();
        if (zkClient.exists(path)) {
            children = zkClient.getChildren(path);
        }
        return children;
    }

    public static List<String> getGroups(ZkClient zkClient) {
        return getChildren(zkClient, ZOOKEEPER_REGISTRY_NAMESPACE);
    }

    @Deprecated
    public static List<String> getProvidersByGroup(ZkClient zkClient, String group) {
        List<String> providers = getChildren(zkClient, getGroupPath(group));
        return providers;
    }

    public static List<AddressInfo> getNodes(ZkClient zkClient, String group, String providerPath, String statusNode) {
        return getNodes(zkClient, group, providerPath, ZookeeperStatusNode.fromValue(statusNode));
    }

    public static List<AddressInfo> getNodes(ZkClient zkClient, String group, String providerPath, ZookeeperStatusNode statusNode) {
        List<AddressInfo> result = new ArrayList<>();
        List<String> nodes = getChildren(zkClient, getStatusNodePath(group, providerPath, statusNode));
        for (String nodeName : nodes) {
            AddressInfo addressInfo = new AddressInfo();
            String info = zkClient.readData(getAddressPath(group, providerPath, statusNode, nodeName), true);
            addressInfo.setAddress(nodeName);
            addressInfo.setContents(info);
            result.add(addressInfo);
        }
        return result;
    }

    public static Map<String, Map<String, List<AddressInfo>>> getAllNodes(ZkClient zkClient, String group) {
        Map<String, Map<String, List<AddressInfo>>> results = new HashMap<>();
        List<String> services = getProvidersByGroup(zkClient, group);
        for (String serviceName : services) {
            Map<String, List<AddressInfo>> service = new HashMap();
            List<AddressInfo> availableServer = getNodes(zkClient, group, serviceName, ZookeeperStatusNode.ACTIVE);
            service.put(ZookeeperStatusNode.ACTIVE.getValue(), availableServer);
            List<AddressInfo> unavailableServer = getNodes(zkClient, group, serviceName, ZookeeperStatusNode.INACTIVE);
            service.put(ZookeeperStatusNode.INACTIVE.getValue(), unavailableServer);
            List<AddressInfo> clientNode = getNodes(zkClient, group, serviceName, ZookeeperStatusNode.CLIENT);
            service.put(ZookeeperStatusNode.CLIENT.getValue(), clientNode);

            results.put(serviceName, service);
        }
        return results;
    }

    /**
     * Get the full path of specified application node
     *
     * @param appName application name
     * @return specified application node full path
     */
    public static String getApplicationPath(String appName) {
        return getGroupPath(Url.PARAM_GROUP_APPLICATION) + PATH_SEPARATOR + appName;
    }

    /**
     * Get the full path of specified application provider
     *
     * @param appName application name
     * @return specified application provider node full path
     */
    public static String getApplicationProviderPath(String appName) {
        return getGroupPath(Url.PARAM_GROUP_APPLICATION_PROVIDER) + PATH_SEPARATOR + appName;
    }

    public static String getApplicationPath() {
        return getGroupPath(Url.PARAM_GROUP_APPLICATION);
    }

    public static List<String> getAllApplications(ZkClient zkClient) {
        return getChildren(zkClient, getGroupPath(Url.PARAM_GROUP_APPLICATION));
    }
}

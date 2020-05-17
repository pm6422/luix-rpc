package org.infinity.rpc.registry.zookeeper.utils;

import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.registry.zookeeper.ZookeeperStatusNode;

import static org.infinity.rpc.core.registry.Url.PATH_SEPARATOR;

public class ZookeeperUtils {

    public static final String ZOOKEEPER_REGISTRY_NAMESPACE = "/infinity";
    public static final String ZOOKEEPER_REGISTRY_COMMAND   = "/command";

    /**
     * Get the provider address full path of specified node
     *
     * @param url  url
     * @param node zookeeper active status node
     * @return address full path
     */
    public static String getAddressPath(Url url, ZookeeperStatusNode node) {
        return getActiveNodePath(url, node) + PATH_SEPARATOR + url.getServerPortStr();
    }

    /**
     * Get the provider status node full path under specified group and status node
     *
     * @param url  url
     * @param node zookeeper active status node
     * @return provider status node full path
     */
    public static String getActiveNodePath(Url url, ZookeeperStatusNode node) {
        return getActiveNodePath(url.getGroup(), url.getPath(), node);
    }

    /**
     * Get the provider status node full path under specified group and status node
     *
     * @param group        zookeeper group node
     * @param providerPath provider class fully-qualified name
     * @param node         status node
     * @return provider status node full path
     */
    public static String getActiveNodePath(String group, String providerPath, ZookeeperStatusNode node) {
        return getProviderPath(group, providerPath) + PATH_SEPARATOR + node.getValue();
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
     * Get the group node full path
     *
     * @param group group
     * @return group node full path
     */
    public static String getGroupPath(String group) {
        return ZOOKEEPER_REGISTRY_NAMESPACE + PATH_SEPARATOR + group;
    }
}

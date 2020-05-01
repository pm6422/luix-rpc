package org.infinity.rpc.registry.zookeeper.utils;

import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.registry.zookeeper.ZookeeperActiveStatusNode;

public class ZookeeperUtils {

    public static final String ZOOKEEPER_REGISTRY_NAMESPACE = "/infinity";
    public static final String ZOOKEEPER_REGISTRY_COMMAND   = "/command";

    /**
     * Get the zookeeper address full path of specified node
     *
     * @param url  url
     * @param node zookeeper active status node
     * @return zookeeper path
     */
    public static String getAddressPath(Url url, ZookeeperActiveStatusNode node) {
        return getActiveNodePath(url, node) + Url.PATH_SEPARATOR + url.getServerPortStr();
    }

    /**
     * Get the zookeeper active status full path of specified node
     *
     * @param url  url
     * @param node zookeeper active status node
     * @return zookeeper path
     */
    public static String getActiveNodePath(Url url, ZookeeperActiveStatusNode node) {
        return getProviderPath(url) + Url.PATH_SEPARATOR + node.getValue();
    }

    /**
     * Get the zookeeper provider full path of specified url
     *
     * @param url url
     * @return zookeeper path
     */
    public static String getProviderPath(Url url) {
        return getGroupPath(url) + Url.PATH_SEPARATOR + url.getPath();
    }

    /**
     * Get the zookeeper command full path of specified url
     *
     * @param url url
     * @return zookeeper path
     */
    public static String getCommandPath(Url url) {
        return getGroupPath(url) + ZOOKEEPER_REGISTRY_COMMAND;
    }

    /**
     * Get the zookeeper group full path of specified url
     *
     * @param url url
     * @return zookeeper path
     */
    public static String getGroupPath(Url url) {
        return ZOOKEEPER_REGISTRY_NAMESPACE + Url.PATH_SEPARATOR + url.getGroup();
    }
}

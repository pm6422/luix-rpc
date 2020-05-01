package org.infinity.rpc.registry.zookeeper.utils;

import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.registry.zookeeper.ZkNodeType;

public class ZkUtils {

    public static final String ZOOKEEPER_REGISTRY_NAMESPACE = "/infinity";
    public static final String ZOOKEEPER_REGISTRY_COMMAND   = "/command";

    /**
     *
     * @param url
     * @param nodeType
     * @return
     */
    public static String getPathByNode(Url url, ZkNodeType nodeType) {
        return toServicePath(url) + Url.PATH_SEPARATOR + nodeType.getValue();
    }

    public static String toServicePath(Url url) {
        return toGroupPath(url) + Url.PATH_SEPARATOR + url.getPath();
    }

    public static String toGroupPath(Url url) {
        return ZOOKEEPER_REGISTRY_NAMESPACE + Url.PATH_SEPARATOR + url.getGroup();
    }

    public static String toCommandPath(Url url) {
        return toGroupPath(url) + ZOOKEEPER_REGISTRY_COMMAND;
    }


    public static String toNodePath(Url url, ZkNodeType nodeType) {
        return getPathByNode(url, nodeType) + Url.PATH_SEPARATOR + url.getServerPortStr();
    }
}

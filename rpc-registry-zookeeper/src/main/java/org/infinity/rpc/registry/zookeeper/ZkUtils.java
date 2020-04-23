package org.infinity.rpc.registry.zookeeper;

import org.infinity.rpc.core.registry.Url;

public class ZkUtils {

    public static final String ZOOKEEPER_REGISTRY_NAMESPACE = "/motan";
    public static final String ZOOKEEPER_REGISTRY_COMMAND = "/command";

    public static String toGroupPath(Url url) {
        return ZOOKEEPER_REGISTRY_NAMESPACE + Url.PATH_SEPARATOR + url.getGroup();
    }

    public static String toServicePath(Url url) {
        return toGroupPath(url) + Url.PATH_SEPARATOR + url.getPath();
    }

    public static String toCommandPath(Url url) {
        return toGroupPath(url) + ZOOKEEPER_REGISTRY_COMMAND;
    }

    public static String toNodeTypePath(Url url, ZkNodeType nodeType) {
        return toServicePath(url) + Url.PATH_SEPARATOR + nodeType.getValue();
    }

    public static String toNodePath(Url url, ZkNodeType nodeType) {
        return toNodeTypePath(url, nodeType) + Url.PATH_SEPARATOR + url.getServerPortStr();
    }
}

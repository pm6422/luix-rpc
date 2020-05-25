package org.infinity.rpc.registry.zookeeper;

import java.util.Arrays;

/**
 * Zookeeper active status node name
 */
public enum ZookeeperStatusNode {

    ONLINE("online"),
    OFFLINE("offline"),
    CLIENT("client");

    ZookeeperStatusNode(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }

    public static ZookeeperStatusNode fromValue(String value) {
        return Arrays.stream(ZookeeperStatusNode.values()).filter(x -> x.getValue().equals(value)).findFirst().orElse(null);
    }
}

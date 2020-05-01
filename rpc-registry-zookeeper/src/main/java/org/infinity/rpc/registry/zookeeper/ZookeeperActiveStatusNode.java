package org.infinity.rpc.registry.zookeeper;

/**
 * Zookeeper active status node name
 */
public enum ZookeeperActiveStatusNode {

    ACTIVE_SERVER("active"),
    INACTIVE_SERVER("inactive"),
    CLIENT("client");

    ZookeeperActiveStatusNode(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}

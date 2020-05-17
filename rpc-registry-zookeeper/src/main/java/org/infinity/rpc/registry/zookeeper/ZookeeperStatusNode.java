package org.infinity.rpc.registry.zookeeper;

/**
 * Zookeeper active status node name
 */
public enum ZookeeperStatusNode {

    ACTIVE("active"),
    INACTIVE("inactive"),
    CLIENT("client");

    ZookeeperStatusNode(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}

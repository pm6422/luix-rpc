package org.infinity.rpc.registry.zookeeper;

/**
 * Zookeeper active status node name
 */
public enum ZookeeperNodeName {

    ACTIVE("active"),
    INACTIVE("inactive"),
    CLIENT("client");

    ZookeeperNodeName(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}

package org.infinity.rpc.registry.zookeeper;

public enum ZkNodeType {

    ACTIVE_SERVER("active"),
    INACTIVE_SERVER("inactive"),
    CLIENT("client");

    private String value;

    private ZkNodeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

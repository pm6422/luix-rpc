package org.infinity.rpc.registry.zookeeper;

public enum ZkNodeType {

    NORMAL_SERVER("normal"),
    ABNORMAL_SERVER("abnormal"),
    CLIENT("client");

    private String value;

    private ZkNodeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

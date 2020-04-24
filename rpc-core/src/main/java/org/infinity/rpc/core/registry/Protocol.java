package org.infinity.rpc.core.registry;


public enum Protocol {
    ZOOKEEPER("zookeeper");

    private String value;

    Protocol(String value) {
        this.value = value;
    }

    public Protocol fromValue(String value) {
        return Protocol.valueOf(value);
    }

    public String value() {
        return value;
    }
}

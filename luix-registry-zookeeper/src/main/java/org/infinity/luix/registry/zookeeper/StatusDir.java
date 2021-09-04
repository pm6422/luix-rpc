package org.infinity.luix.registry.zookeeper;

import java.util.Arrays;

public enum StatusDir {

    ACTIVE("active"),
    INACTIVE("inactive"),
    CONSUMING("consuming");

    StatusDir(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }

    public static StatusDir fromValue(String value) {
        return Arrays.stream(StatusDir.values()).filter(x -> x.getValue().equals(value)).findFirst().orElse(null);
    }
}

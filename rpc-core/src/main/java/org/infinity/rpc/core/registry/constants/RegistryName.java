package org.infinity.rpc.core.registry.constants;

public enum RegistryName {
    zookeeper("zookeeper");

    private String value;

    RegistryName(String value) {
        this.value = value;
    }

    public RegistryName fromName(String name) {
        return RegistryName.valueOf(name);
    }

    public String getValue() {
        return value;
    }
}
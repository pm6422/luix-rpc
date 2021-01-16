package org.infinity.rpc.core.registry.constants;

import org.infinity.rpc.utilities.lang.EnumValueHoldable;

public enum RegistryName implements EnumValueHoldable<String> {
    zookeeper("zookeeper"),
    local("local"),
    direct("direct");

    private final String value;

    RegistryName(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
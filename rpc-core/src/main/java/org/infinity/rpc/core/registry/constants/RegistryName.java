package org.infinity.rpc.core.registry.constants;

import org.infinity.rpc.utilities.lang.EnumValueFieldHoldable;

public enum RegistryName implements EnumValueFieldHoldable {
    zookeeper("zookeeper");

    private String value;

    RegistryName(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
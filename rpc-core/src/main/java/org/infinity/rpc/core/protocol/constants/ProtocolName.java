package org.infinity.rpc.core.protocol.constants;

import org.infinity.rpc.utilities.lang.EnumValueHoldable;

public enum ProtocolName implements EnumValueHoldable<String> {
    infinity("infinity");

    private final String value;

    ProtocolName(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
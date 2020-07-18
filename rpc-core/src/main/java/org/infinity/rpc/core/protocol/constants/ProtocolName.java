package org.infinity.rpc.core.protocol.constants;

import org.infinity.rpc.utilities.lang.EnumValueFieldHoldable;

public enum ProtocolName implements EnumValueFieldHoldable {
    infinity("infinity");

    private String value;

    ProtocolName(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
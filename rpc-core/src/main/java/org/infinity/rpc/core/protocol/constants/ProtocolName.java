package org.infinity.rpc.core.protocol.constants;

public enum ProtocolName {
    infinity("infinity");

    private String value;

    ProtocolName(String value) {
        this.value = value;
    }

    public ProtocolName fromName(String name) {
        return ProtocolName.valueOf(name);
    }

    public String getValue() {
        return value;
    }
}
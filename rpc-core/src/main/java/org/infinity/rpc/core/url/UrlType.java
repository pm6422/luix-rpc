package org.infinity.rpc.core.url;

import org.infinity.rpc.utilities.lang.EnumValueHoldable;

public enum UrlType implements EnumValueHoldable {
    PROVIDER("provider"),
    REGISTRY("registry"),
    CLIENT("client");

    private String value;

    UrlType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
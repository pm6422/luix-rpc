package org.infinity.rpc.core.url;

import org.infinity.rpc.utilities.lang.EnumValueFieldHoldable;

public enum UrlType implements EnumValueFieldHoldable {
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
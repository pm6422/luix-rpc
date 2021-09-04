package org.infinity.luix.core.url;

import org.infinity.luix.utilities.lang.EnumValueHoldable;

public enum UrlType implements EnumValueHoldable<String> {
    PROVIDER("provider"),
    REGISTRY("registry"),
    CLIENT("client");

    private final String value;

    UrlType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
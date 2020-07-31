package org.infinity.rpc.core.client.proxy;

import org.infinity.rpc.utilities.lang.EnumValueFieldHoldable;

public enum AsyncRequestFlag implements EnumValueFieldHoldable {
    ASYNC("async"),
    SYNC("sync");

    private String value;

    AsyncRequestFlag(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
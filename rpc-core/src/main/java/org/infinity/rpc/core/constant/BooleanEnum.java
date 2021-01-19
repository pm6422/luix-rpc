package org.infinity.rpc.core.constant;

import org.infinity.rpc.utilities.lang.EnumValueHoldable;

public enum BooleanEnum implements EnumValueHoldable<Boolean> {
    NULL(null),
    TRUE(true),
    FALSE(false);

    private final Boolean value;

    BooleanEnum(Boolean value) {
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }
}
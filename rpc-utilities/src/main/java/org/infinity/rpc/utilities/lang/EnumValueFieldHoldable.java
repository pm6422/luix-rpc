package org.infinity.rpc.utilities.lang;

import org.apache.commons.lang3.EnumUtils;

public interface EnumValueFieldHoldable<T> {
    T getValue();

    static <E extends Enum<E> & EnumValueFieldHoldable<T>, T> boolean isValidValue(Class<E> clazz, T value) {
        return getEnumFromValue(clazz, value) != null;
    }

    static <E extends Enum<E> & EnumValueFieldHoldable<T>, T> E getEnumFromValue(Class<E> clazz, T value) {
        return value == null ? null :
                EnumUtils.getEnumList(clazz)
                        .stream()
                        .filter(e -> value.equals(e.getValue()))
                        .findFirst()
                        .orElse(null);
    }
}

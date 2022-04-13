package com.luixtech.rpc.core.utils;

import com.luixtech.rpc.core.exception.impl.RpcConfigException;

/**
 * Refer to {@link org.apache.commons.lang3.Validate}
 */
public abstract class RpcConfigValidator {

    public static void isTrue(final boolean expression, final String message, final Object... values) {
        if (!expression) {
            throw new RpcConfigException(String.format(message, values));
        }
    }

    public static <T extends CharSequence> T notEmpty(final T chars, final String message, final Object... values) {
        if (chars == null) {
            throw new RpcConfigException(String.format(message, values));
        }
        if (chars.length() == 0) {
            throw new RpcConfigException(String.format(message, values));
        }
        return chars;
    }

    public static <T extends CharSequence> T mustEmpty(final T chars, final String message, final Object... values) {
        if (chars != null) {
            throw new RpcConfigException(String.format(message, values));
        }
        return chars;
    }

    public static <T> T notNull(final T object, final String message, final Object... values) {
        if (object == null) {
            throw new RpcConfigException(String.format(message, values));
        }
        return object;
    }

    public static <T> T mustNull(final T object, final String message, final Object... values) {
        if (object != null) {
            throw new RpcConfigException(String.format(message, values));
        }
        return object;
    }
}

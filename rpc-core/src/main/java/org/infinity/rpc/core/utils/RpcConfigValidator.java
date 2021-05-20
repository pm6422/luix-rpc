package org.infinity.rpc.core.utils;

import org.infinity.rpc.core.exception.impl.RpcConfigurationException;

/**
 * Refer to {@link org.apache.commons.lang3.Validate}
 */
public abstract class RpcConfigValidator {

    public static void isTrue(final boolean expression, final String message, final Object... values) {
        if (!expression) {
            throw new RpcConfigurationException(String.format(message, values));
        }
    }

    public static <T extends CharSequence> T notEmpty(final T chars, final String message, final Object... values) {
        if (chars == null) {
            throw new RpcConfigurationException(String.format(message, values));
        }
        if (chars.length() == 0) {
            throw new RpcConfigurationException(String.format(message, values));
        }
        return chars;
    }

    public static <T extends CharSequence> T mustEmpty(final T chars, final String message, final Object... values) {
        if (chars != null && chars.length() >= 0) {
            throw new RpcConfigurationException(String.format(message, values));
        }
        return chars;
    }

    public static <T> T notNull(final T object, final String message, final Object... values) {
        if (object == null) {
            throw new RpcConfigurationException(String.format(message, values));
        }
        return object;
    }

    public static <T> T mustNull(final T object, final String message, final Object... values) {
        if (object != null) {
            throw new RpcConfigurationException(String.format(message, values));
        }
        return object;
    }

}

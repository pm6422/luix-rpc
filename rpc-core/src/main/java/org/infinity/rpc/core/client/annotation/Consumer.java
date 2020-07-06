package org.infinity.rpc.core.client.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC consumer annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Consumer {
    Class<?> interfaceClass() default void.class;

    /**
     * Timeout value for service invocation, default value is 0
     */
    int timeout() default 0;
}
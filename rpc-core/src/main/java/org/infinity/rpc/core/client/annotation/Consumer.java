package org.infinity.rpc.core.client.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC consumer annotation
 * Please refer {@link org.infinity.rpc.core.constant.ConsumerProviderAnnotationAttributes} for the property definition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Consumer {
    // The interface class of consumer class
    Class<?> interfaceClass() default void.class;

    // The interface class name of consumer class
    // For the generic consumer instance must provide interfaceName attribute
    String interfaceName() default "";

    // The generic call indicator,
    // if we only have the provider interface name, method name and arguments,
    // we can initiate a generic call to service provider without service provider jar dependency
    boolean generic() default false;

    String group() default "";

    String version() default "";

    // Timeout value for service invocation
    int timeout() default 0;

    // Provider url used to connect provider directly without third party registry
    String directUrl() default "";
}
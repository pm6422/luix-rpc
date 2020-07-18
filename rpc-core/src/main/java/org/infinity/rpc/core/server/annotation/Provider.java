package org.infinity.rpc.core.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC provider annotation
 * Please refer {@link org.infinity.rpc.core.constant.ConsumerProviderAnnotationAttributes} for the property definition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Provider {
    Class<?> interfaceClass() default void.class;

    // The interface class name of consumer class
    // For the generic consumer instance must provide interfaceName attribute
    String interfaceName() default "";

    // Timeout value for service invocation
    int timeout() default 0;
}

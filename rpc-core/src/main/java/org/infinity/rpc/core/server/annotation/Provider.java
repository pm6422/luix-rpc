package org.infinity.rpc.core.server.annotation;

import org.infinity.rpc.core.constant.ServiceConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC provider annotation
 * Please refer {@link ServiceConstants} for the property definition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Provider {
    // The interface class name of provider
    // For the generic call must provide interfaceName attribute
    String interfaceName() default "";

    // Interface class of provider
    Class<?> interfaceClass() default void.class;

    String protocol() default "";

    /**
     * Available values: [zookeeper]
     *
     * @return registry
     */
//    String registry() default "";

    String group() default "";

    String version() default "";

    String checkHealthFactory() default "";

    // Timeout value for service invocation
    int requestTimeout() default Integer.MAX_VALUE;

    // The max retry times of RPC request
    int maxRetries() default Integer.MAX_VALUE;
}

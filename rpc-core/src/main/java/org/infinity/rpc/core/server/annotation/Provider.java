package org.infinity.rpc.core.server.annotation;

import org.infinity.rpc.core.constant.ServiceConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.infinity.rpc.core.constant.ServiceConstants.GROUP_DEFAULT_VALUE;
import static org.infinity.rpc.core.constant.ServiceConstants.VERSION_DEFAULT_VALUE;

/**
 * RPC provider annotation
 * Please refer {@link ServiceConstants} for the property definition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Provider {
    // Interface class of provider
    Class<?> interfaceClass() default void.class;

    // The interface class name of provider
    // For the generic call must provide interfaceName attribute
    String interfaceName() default "";

    String group() default GROUP_DEFAULT_VALUE;

    String version() default VERSION_DEFAULT_VALUE;

    // Timeout value for service invocation
    int timeout() default 0;

    // The max retry times of RPC request
    int maxRetries() default 0;

    // Indicator to monitor health
    boolean checkHealth() default true;
}

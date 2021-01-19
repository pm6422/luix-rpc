package org.infinity.rpc.core.server.annotation;

import org.infinity.rpc.core.constant.ServiceConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.infinity.rpc.core.constant.ServiceConstants.*;

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

    String registry() default REGISTRY_DEFAULT_VALUE;

    String protocol() default PROTOCOL_DEFAULT_VALUE;

    String group() default GROUP_DEFAULT_VALUE;

    String version() default VERSION_DEFAULT_VALUE;

    // Indicator to monitor health
    boolean checkHealth() default CHECK_HEALTH_DEFAULT_VALUE;

    String checkHealthFactory() default CHECK_HEALTH_FACTORY_DEFAULT_VALUE;

    // Timeout value for service invocation
    int timeout() default REQUEST_TIMEOUT_DEFAULT_VALUE;

    // The max retry times of RPC request
    int maxRetries() default MAX_RETRIES_DEFAULT_VALUE;

}

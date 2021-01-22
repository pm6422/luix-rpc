package org.infinity.rpc.core.client.annotation;

import org.infinity.rpc.core.constant.BooleanEnum;
import org.infinity.rpc.core.constant.ServiceConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC consumer annotation
 * Please refer {@link ServiceConstants} for the property definition
 * <p>
 * This class can annotate non-static field, non-static public method with prefix 'set' name and one parameter
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

    String registry() default "";

    String protocol() default "";

    String cluster() default "";

    String faultTolerance() default "";

    String loadBalancer() default "";

    String group() default "";

    String version() default "";

    // Indicator to monitor health
    BooleanEnum checkHealth() default BooleanEnum.NULL;

    String checkHealthFactory() default "";

    // Timeout value for service invocation
    int requestTimeout() default Integer.MAX_VALUE;

    // The max retry times of RPC request
    int maxRetries() default Integer.MAX_VALUE;

    // Provider url used to connect provider directly without third party registry
    String directUrl() default "";
}
package org.infinity.rpc.core.client.annotation;

import org.infinity.rpc.core.constant.ServiceConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;

/**
 * RPC consumer annotation
 * Please refer {@link ServiceConstants} for the property definition
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

    String registry() default REGISTRY_DEFAULT_VALUE;

    String protocol() default PROTOCOL_DEFAULT_VALUE;

    String cluster() default CLUSTER_DEFAULT_VALUE;

    String loadBalancer() default LOAD_BALANCER_DEFAULT_VALUE;

    String faultTolerance() default FAULT_TOLERANCE_DEFAULT_VALUE;

    String group() default GROUP_DEFAULT_VALUE;

    String version() default VERSION_DEFAULT_VALUE;

    // Indicator to monitor health
    boolean checkHealth() default CHECK_HEALTH_DEFAULT_VALUE;

    String checkHealthFactory() default CHECK_HEALTH_FACTORY_DEFAULT_VALUE;

    // Timeout value for service invocation
    int timeout() default 0;

    // Provider url used to connect provider directly without third party registry
    String directUrl() default "";
}
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
    /**
     * The interface class of RPC consumer class
     *
     * @return interface class
     */
    Class<?> interfaceClass() default void.class;

    /**
     * The interface class fully-qualified name of RPC consumer class
     * For the generic consumer instance must specify interfaceName attribute
     *
     * @return fully-qualified class name
     */
    String interfaceName() default "";

    /**
     * Available values: [infinity, zookeeper, local, direct, injvm]
     * @return protocol
     */
    String protocol() default "";

    /**
     * Available values: [zookeeper]
     * @return registry
     */
//    String registry() default "";

    /**
     * @return provider calling cluster
     */
    String cluster() default "";

    /**
     * @return fault tolerance
     */
    String faultTolerance() default "";

    /**
     * @return load balancer
     */
    String loadBalancer() default "";

    /**
     * @return group
     */
    String group() default "";

    /**
     * @return version
     */
    String version() default "";

    /**
     * Indicator to check health
     *
     * @return check health indicator
     */
    BooleanEnum checkHealth() default BooleanEnum.NULL;

    /**
     * Enabled when checkHealth is true
     *
     * @return check health factory
     */
    String checkHealthFactory() default "";

    /**
     * @return Timeout value for service invocation
     */
    int requestTimeout() default Integer.MAX_VALUE;

    /**
     * @return The max retry times of RPC request
     */
    int maxRetries() default Integer.MAX_VALUE;

    /**
     * Addresses of RPC provider used to connect RPC provider directly without third party registry.
     * Multiple addresses are separated by comma.
     *
     * @return direct urls, e.g. 127.0.0.1:26010,192.168.120.111:26010
     */
    String directAddresses() default "";

    /**
     * The generic call indicator
     * if we only have the provider interface name, method name and arguments,
     * we can initiate a generic call to service provider without service provider jar dependency
     *
     * @return generic call
     */
    boolean generic() default false;
}
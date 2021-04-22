package org.infinity.rpc.core.client.annotation;

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
     * The interface class fully-qualified name of RPC consumer class.
     * Especially for the generic consumer instance must specify interfaceName attribute
     *
     * @return fully-qualified class name
     */
    String interfaceName() default "";

    /**
     * Interface class of RPC consumer
     *
     * @return interface class
     */
    Class<?> interfaceClass() default void.class;

    /**
     * Protocol used to handle RPC request
     * Available values: [infinity, zookeeper, direct]
     *
     * @return protocol
     */
    String protocol() default "";

    /**
     * One service interface may have multiple implementations(forms),
     * It used to distinguish between different implementations of service provider interface
     *
     * @return form service implementation
     */
    String form() default "";

    /**
     * When the service changes, such as adding or deleting methods, and interface parameters change,
     * the provider and consumer application instances need to be upgraded.
     * In order to deploy in a production environment without affecting user use,
     * a gradual migration scheme is generally adopted.
     * First upgrade some provider application instances,
     * and then use the same version number to upgrade some consumer instances.
     * The old version of the consumer instance calls the old version of the provider instance.
     * Observe that there is no problem and repeat this process to complete the upgrade.
     *
     * @return version
     */
    String version() default "";

    /**
     * Provider invocation cluster
     *
     * @return provider invocation cluster
     */
    String cluster() default "";

    /**
     * Fault tolerance strategy
     *
     * @return fault tolerance
     */
    String faultTolerance() default "";

    /**
     * Load balancer
     *
     * @return load balancer
     */
    String loadBalancer() default "";

    /**
     * Used to create proxyInstance which is the implementation of consumer interface class
     *
     * @return consumer proxy factory
     */
    String proxyFactory() default "";

    /**
     * Provider health checker
     *
     * @return health checker
     */
    String healthChecker() default "";

    /**
     * RPC invocation timeout in milliseconds
     *
     * @return timeout
     */
    int requestTimeout() default Integer.MAX_VALUE;

    /**
     * The max retry times of RPC invocation
     *
     * @return max retry times
     */
    int maxRetries() default Integer.MAX_VALUE;

    /**
     * Addresses of RPC provider used to connect RPC provider directly without third party registry.
     * Multiple addresses are separated by comma.
     *
     * @return direct urls, e.g. 127.0.0.1:26010,192.168.120.111:26010
     */
    String providerAddresses() default "";
}
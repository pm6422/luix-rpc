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

    /**
     * Available values: [infinity, zookeeper, local, direct, injvm]
     *
     * @return protocol
     */
    String protocol() default "";

    /**
     * Available values: [zookeeper]
     *
     * @return registry
     */
//    String registry() default "";

    /**
     * One service interface may have multiple implementations(forms),
     * It used to distinguish between different implementations of service provider interface
     *
     * @return group
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
     * @return health checker
     */
    String healthChecker() default "";

    // Timeout value for service invocation
    int requestTimeout() default Integer.MAX_VALUE;

    // The max retry times of RPC request
    int maxRetries() default Integer.MAX_VALUE;
}

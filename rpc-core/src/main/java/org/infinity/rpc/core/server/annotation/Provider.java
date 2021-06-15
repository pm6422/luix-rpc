package org.infinity.rpc.core.server.annotation;

import org.infinity.rpc.core.constant.ServiceConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC provider annotation
 * Please refer {@link ServiceConstants} for the property definition
 * <p>
 * The method level takes precedence, the interface level is second, and the global configuration is again.
 * If the level is the same, the consumer will be given priority, and the provider will be second.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Provider {
    /**
     * The interface class fully-qualified name of RPC provider class
     *
     * @return fully-qualified class name
     */
    String interfaceName() default "";

    /**
     * Interface class of RPC provider
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
     * Provider health checker
     *
     * @return health checker
     */
    String healthChecker() default "";

    // Timeout value for service invocation

    /**
     * RPC invocation timeout in milliseconds
     * Format: integer
     *
     * @return timeout
     */
    String requestTimeout() default "";

    /**
     * The max retry times of RPC invocation
     * Format: integer
     *
     * @return max retry times
     */
    String maxRetries() default "";
}

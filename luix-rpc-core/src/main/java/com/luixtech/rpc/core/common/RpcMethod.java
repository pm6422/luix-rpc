package com.luixtech.rpc.core.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method level configuration annotation
 * <p>
 * The method level takes precedence, the interface level is second, and the global configuration is again.
 * If the level is the same, the consumer will be given priority, and the provider will be second.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RpcMethod {
    /**
     * RPC invocation timeout in milliseconds
     * Format: integer
     *
     * @return timeout
     */
    String requestTimeout() default "";

    /**
     * The max retry count of RPC invocation
     * Format: integer
     *
     * @return max retry count
     */
    String retryCount() default "";
}

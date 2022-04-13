package com.luixtech.luixrpc.spring.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RpcServiceScanRegistrar.class)
public @interface EnableLuixRpc {

    /**
     * Base packages to scan for annotated {@link com.luixtech.luixrpc.core.server.annotation.RpcProvider}
     * and {@link com.luixtech.luixrpc.core.client.annotation.RpcConsumer} classes.
     *
     * @return the base packages to scan
     */
    String[] scanBasePackages() default {};
}

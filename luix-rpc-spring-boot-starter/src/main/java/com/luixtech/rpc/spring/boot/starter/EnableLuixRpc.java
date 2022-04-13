package com.luixtech.rpc.spring.boot.starter;

import com.luixtech.rpc.core.client.annotation.RpcConsumer;
import com.luixtech.rpc.core.server.annotation.RpcProvider;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RpcServiceScanRegistrar.class)
public @interface EnableLuixRpc {

    /**
     * Base packages to scan for annotated {@link RpcProvider}
     * and {@link RpcConsumer} classes.
     *
     * @return the base packages to scan
     */
    String[] scanBasePackages() default {};
}

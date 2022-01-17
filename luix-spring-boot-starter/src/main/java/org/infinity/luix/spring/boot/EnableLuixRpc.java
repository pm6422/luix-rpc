package org.infinity.luix.spring.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RpcServiceScanRegistrar.class)
public @interface EnableLuixRpc {

    /**
     * Base packages to scan for annotated {@link org.infinity.luix.core.server.annotation.RpcProvider}
     * and {@link org.infinity.luix.core.client.annotation.RpcConsumer} classes.
     *
     * @return the base packages to scan
     */
    String[] scanBasePackages() default {};
}

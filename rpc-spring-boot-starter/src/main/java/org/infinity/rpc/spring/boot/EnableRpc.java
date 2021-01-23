package org.infinity.rpc.spring.boot;

import org.infinity.rpc.spring.boot.registrar.RpcProviderConsumerScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(RpcProviderConsumerScanRegistrar.class)
public @interface EnableRpc {

    String[] scanBasePackages() default {};
}

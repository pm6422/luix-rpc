package org.infinity.springboot.infinityrpc;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({RpcClientAutoConfiguration.class})
public @interface EnableRpc {
}

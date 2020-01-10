package org.infinity.rpc.client.registrar;

import org.infinity.rpc.client.RpcClientConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({RpcConsumerRegistrar.class, RpcClientConfiguration.class})
public @interface EnableRpcClient {
}

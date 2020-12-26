package org.infinity.rpc.core.client.proxy;


import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.client.ConsumerWrapper;

import java.lang.reflect.Proxy;

@Slf4j
public class ConsumerProxy {
    /**
     * @param wrapper Consumer wrapper
     * @param <T>     The interface class of the consumer
     * @return The consumer proxy instance
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T getProxy(ConsumerWrapper<T> wrapper) {
        Object proxy = Proxy.newProxyInstance(
                wrapper.getInterfaceClass().getClassLoader(),
                new Class<?>[]{wrapper.getInterfaceClass()},
                new ConsumerInvocationHandler<>(wrapper));
        return (T) proxy;
    }
}







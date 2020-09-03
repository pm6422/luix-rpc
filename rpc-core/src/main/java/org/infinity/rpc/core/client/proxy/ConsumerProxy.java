package org.infinity.rpc.core.client.proxy;


import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.springframework.util.Assert;

import java.lang.reflect.Proxy;

@Slf4j
public class ConsumerProxy {
    /**
     * @param interfaceClass     The interface class of the consumer
     * @param infinityProperties properties
     * @param <T>                The interface class of the consumer
     * @return The consumer proxy instance
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T getProxy(Class<T> interfaceClass, InfinityProperties infinityProperties) {
        Assert.notNull(interfaceClass, "Consumer interface class must not be null!");
        Object proxy = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ConsumerInvocationHandler<>(interfaceClass, infinityProperties));
        return (T) proxy;
    }
}







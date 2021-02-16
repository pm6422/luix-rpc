package org.infinity.rpc.core.client.proxy;


import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.stub.ConsumerStub;

import java.lang.reflect.Proxy;

@Slf4j
public class ConsumerProxy {
    /**
     * @param stub Consumer stub
     * @param <T>     The interface class of the consumer
     * @return The consumer proxy instance
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T getProxy(ConsumerStub<T> stub) {
        Object proxy = Proxy.newProxyInstance(
                stub.getInterfaceClass().getClassLoader(),
                new Class<?>[]{stub.getInterfaceClass()},
                new ConsumerInvocationHandler<>(stub));
        return (T) proxy;
    }
}







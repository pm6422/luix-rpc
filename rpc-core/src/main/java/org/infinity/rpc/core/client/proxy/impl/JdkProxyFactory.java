package org.infinity.rpc.core.client.proxy.impl;


import org.infinity.rpc.core.client.invocationhandler.GenericInvocationHandler;
import org.infinity.rpc.core.client.invocationhandler.impl.ConsumerInvocationHandler;
import org.infinity.rpc.core.client.proxy.ProxyFactory;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.lang.reflect.Proxy;

import static org.infinity.rpc.core.constant.ConsumerConstants.PROXY_FACTORY_VAL_JDK;

@SpiName(PROXY_FACTORY_VAL_JDK)
public class JdkProxyFactory implements ProxyFactory {
    /**
     * Get implementation proxy of consumer interface class
     *
     * @param stub Consumer stub
     * @param <T>  The interface class of the consumer
     * @return The consumer proxy instance
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public <T> T getProxy(ConsumerStub<T> stub) {
        Object proxy = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{stub.getInterfaceClass()},
                new ConsumerInvocationHandler<>(stub));
        return (T) proxy;
    }

    @Override
    public GenericInvocationHandler createGenericInvokeHandler(ConsumerStub<?> stub) {
        return new ConsumerInvocationHandler<>(stub);
    }
}







package com.luixtech.rpc.core.client.proxy.impl;


import com.luixtech.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.rpc.core.client.invocationhandler.impl.ConsumerInvocationHandler;
import com.luixtech.rpc.core.client.proxy.Proxy;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.utilities.serviceloader.annotation.SpiName;

import static com.luixtech.rpc.core.constant.ConsumerConstants.PROXY_VAL_JDK;

@SpiName(PROXY_VAL_JDK)
public class JdkProxy implements Proxy {
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
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{stub.getInterfaceClass()},
                new ConsumerInvocationHandler<>(stub));
        return (T) proxy;
    }

    /**
     * Create universal RPC invocation handler
     *
     * @param stub Consumer stub
     * @return Universal RPC invocation handler
     */
    @Override
    public UniversalInvocationHandler createUniversalInvocationHandler(ConsumerStub<?> stub) {
        return new ConsumerInvocationHandler<>(stub);
    }
}







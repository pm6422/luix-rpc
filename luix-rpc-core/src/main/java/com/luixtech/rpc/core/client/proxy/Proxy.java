package com.luixtech.rpc.core.client.proxy;

import com.luixtech.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import com.luixtech.utilities.serviceloader.annotation.Spi;
import com.luixtech.utilities.serviceloader.annotation.SpiScope;

@Spi(scope = SpiScope.PROTOTYPE)
public interface Proxy {
    /**
     * Get implementation proxy of consumer interface class
     *
     * @param stub Consumer stub
     * @param <T>  The interface class of the consumer
     * @return The consumer proxy instance
     */
    <T> T getProxy(ConsumerStub<T> stub);

    /**
     * Create universal RPC invocation handler
     *
     * @param stub Consumer stub
     * @return universal invocation handler
     */
    UniversalInvocationHandler createUniversalInvocationHandler(ConsumerStub<?> stub);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static Proxy getInstance(String name) {
        return ServiceLoader.forClass(Proxy.class).load(name);
    }
}

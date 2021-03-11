package org.infinity.rpc.core.client.proxy;

import org.infinity.rpc.core.client.invocationhandler.GenericInvocationHandler;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

@Spi(scope = SpiScope.PROTOTYPE)
public interface ProxyFactory {
    /**
     * Get implementation proxy of consumer interface class
     *
     * @param stub Consumer stub
     * @param <T>  The interface class of the consumer
     * @return The consumer proxy instance
     */
    <T> T getProxy(ConsumerStub<T> stub);

    /**
     * Create generic call handler
     *
     * @param stub Consumer stub
     * @return generic call handler
     */
    GenericInvocationHandler createGenericCallHandler(ConsumerStub<?> stub);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static ProxyFactory getInstance(String name) {
        return ServiceLoader.forClass(ProxyFactory.class).load(name);
    }
}

package org.infinity.rpc.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

/**
 * Refer to MongoRepositoryFactoryBean
 *
 * @param <T>
 */
@Slf4j
public class RpcConsumerFactoryBean<T> implements FactoryBean<T> {

    private final Class<T>         consumerInterface;
    private       RpcConsumerProxy rpcConsumerProxy;

    public RpcConsumerFactoryBean(Class<T> consumerInterface) {
        Assert.notNull(consumerInterface, "Consumer interface must not be null!");
        this.consumerInterface = consumerInterface;
    }

    public void setRpcConsumerProxy(RpcConsumerProxy rpcConsumerProxy) {
        this.rpcConsumerProxy = rpcConsumerProxy;
    }

    @Override
    public T getObject() throws Exception {
        return rpcConsumerProxy.getProxy(consumerInterface);
    }

    @Override
    public Class<T> getObjectType() {
        return consumerInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}


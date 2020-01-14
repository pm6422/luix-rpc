package org.infinity.rpc.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * Refer to MongoRepositoryFactoryBean
 *
 * @param <T>
 */
@Slf4j
public class RpcConsumerFactoryBean<T> {

    private final Class<T>         consumerInterface;
    private final RpcConsumerProxy rpcConsumerProxy;

    public RpcConsumerFactoryBean(Class<T> consumerInterface, RpcConsumerProxy rpcConsumerProxy) {
        Assert.notNull(consumerInterface, "Consumer interface must not be null!");
        this.consumerInterface = consumerInterface;
        this.rpcConsumerProxy = rpcConsumerProxy;
    }

    public T getObject() throws Exception {
        return rpcConsumerProxy.getProxy(consumerInterface);
    }
}


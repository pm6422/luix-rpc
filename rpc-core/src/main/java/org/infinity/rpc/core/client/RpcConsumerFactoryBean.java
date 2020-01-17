package org.infinity.rpc.core.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.springframework.util.Assert;

/**
 * Refer to MongoRepositoryFactoryBean
 *
 * @param <T>
 */
@Slf4j
public class RpcConsumerFactoryBean<T> {

    public T getObject(RpcConsumerProxy rpcConsumerProxy, Class<T> consumerInterface) throws Exception {
        Assert.notNull(rpcConsumerProxy, "RPC consumer proxy must not be null!");
        Assert.notNull(consumerInterface, "Consumer interface class must not be null!");
        return rpcConsumerProxy.getProxy(consumerInterface);
    }
}


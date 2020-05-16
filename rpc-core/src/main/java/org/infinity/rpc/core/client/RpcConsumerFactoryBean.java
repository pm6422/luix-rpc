package org.infinity.rpc.core.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;

/**
 * Refer to MongoRepositoryFactoryBean
 *
 * @param <T>
 */
@Slf4j
public class RpcConsumerFactoryBean<T> {

    public T getObject(RpcConsumerProxy rpcConsumerProxy, Class<T> consumerInterface) throws Exception {
        Validate.notNull(rpcConsumerProxy, "RPC consumer proxy must not be null!");
        Validate.notNull(consumerInterface, "Consumer interface class must not be null!");
        return rpcConsumerProxy.getProxy(consumerInterface);
    }
}


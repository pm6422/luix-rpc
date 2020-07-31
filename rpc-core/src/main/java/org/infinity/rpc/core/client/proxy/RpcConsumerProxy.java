package org.infinity.rpc.core.client.proxy;


import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.registry.Registry;
import org.springframework.util.Assert;

import java.lang.reflect.Proxy;
import java.util.List;

@Slf4j
public class RpcConsumerProxy<T> {

    public T getProxy(Class<T> interfaceClass, List<Cluster<T>> clusters, List<Registry> registries) {
        Assert.notNull(interfaceClass, "Consumer interface class must not be null!");
        Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new ConsumerInvocationHandler(interfaceClass, clusters, registries));
        return (T) proxy;
    }
}







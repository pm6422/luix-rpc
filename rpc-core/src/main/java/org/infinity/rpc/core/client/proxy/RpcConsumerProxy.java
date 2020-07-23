package org.infinity.rpc.core.client.proxy;


import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.core.client.RpcClient;
import org.infinity.rpc.core.registry.Registry;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

@Slf4j
public class RpcConsumerProxy {
    private             List<Registry> registries;

    public RpcConsumerProxy(List<Registry> registries) {
        this.registries = registries;
    }

    public <T> T getProxy(Class<T> interfaceClass) {
        Assert.notNull(interfaceClass, "Consumer interface class must not be null!");
        Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, new ConsumerInvocationHandler());
        return (T) proxy;
    }

    class ConsumerInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // TODO: replace with new logic
            new RpcConsumerInvocationHandler().invoke(proxy, method, args);

            if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
                // Object proxy = result.getProxy(consumerInterface.getClassLoader()); 在IDE上光标放到proxy就会看到调用toString()
                log.trace("Invoked Object.toString() by view proxy instance on IDE debugger");
                return ClassUtils.getShortNameAsProperty(RpcConsumerProxy.class);
            }

            // Create request object, including class name, method, parameter types, arguments
            RpcRequest rpcRequest = new RpcRequest(UUID.randomUUID().toString(), method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes(), args);
            log.debug("RPC request: {}", rpcRequest);
            // Create client object and send message to server side
            RpcClient rpcClient = new RpcClient(rpcRequest, registries);
            RpcResponse rpcResponse = rpcClient.send();
            // Get response
            return rpcResponse.getResult();
        }
    }
}







package org.infinity.rpc.core.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.core.client.RpcClient;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.registry.Registry;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@Slf4j
@Deprecated
public class ConsumerInvocationHandler1<T> implements InvocationHandler {
    private final Class<T>           interfaceClass;
    @Deprecated
    private final List<Registry>     registries;
    private final InfinityProperties infinityProperties;

    public ConsumerInvocationHandler1(Class<T> interfaceClass, List<Registry> registries, InfinityProperties infinityProperties) {
        this.interfaceClass = interfaceClass;
        this.registries = registries;
        this.infinityProperties = infinityProperties;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
            // Object proxy = result.getProxy(consumerInterface.getClassLoader()); 在IDE上光标放到proxy就会看到调用toString()
            // IDE may call the Object.toString() method if you set some break pointers.
            log.trace("Invoked Object.toString() by view proxy instance on IDE debugger");
            return ClassUtils.getShortNameAsProperty(ConsumerProxy.class);
        }

        new ConsumerInvocationHandler(interfaceClass, infinityProperties).invoke(proxy, method, args);

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
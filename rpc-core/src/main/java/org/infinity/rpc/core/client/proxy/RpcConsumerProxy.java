package org.infinity.rpc.core.client.proxy;


import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.core.client.RpcClient;
import org.infinity.rpc.core.registry.Registry;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@Slf4j
public class RpcConsumerProxy {
    public static final String         CONSUMER_PROXY_BEAN = "ConsumerProxyBean";
    private             List<Registry> registries;

    public RpcConsumerProxy(List<Registry> registries) {
        this.registries = registries;
    }

    public <T> T getProxy(Class<T> interfaceClass) {
        Assert.notNull(interfaceClass, "Consumer interface class must not be null!");
        ProxyFactory factory = new ProxyFactory();
        factory.setInterfaces(interfaceClass);
        factory.addAdvice(new MethodInvokingMethodInterceptor());
        Object proxy = factory.getProxy(interfaceClass.getClassLoader());
        return (T) proxy;
    }

    class MethodInvokingMethodInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method method = invocation.getMethod();
            if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
                // Object proxy = result.getProxy(consumerInterface.getClassLoader()); 在IDE上光标放到proxy就会看到调用toString()
                log.trace("Invoked Object.toString() by view proxy instance on IDE debugger");
                return ClassUtils.getShortNameAsProperty(RpcConsumerProxy.class);
            }

            // Create request object, including class name, method, parameter types, arguments
            RpcRequest rpcRequest = new RpcRequest(UUID.randomUUID().toString(), method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes(), invocation.getArguments());
            log.debug("RPC request: {}", rpcRequest);
            // Create client object and send message to server side
            RpcClient rpcClient = new RpcClient(rpcRequest, registries);
            RpcResponse rpcResponse = rpcClient.send();
            // Get response
            return rpcResponse.getResult();
        }
    }
}







package org.infinity.rpc.client;


import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.registry.ZkRegistryRpcServerDiscovery;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 获取到接口对应的代理对象，被代理对象的所有的方法调用时都会执行invoke方法
 */
@Slf4j
public class RpcConsumerProxy {
    private              ZkRegistryRpcServerDiscovery rpcServerDiscovery;

    public RpcConsumerProxy(ZkRegistryRpcServerDiscovery zkRegistryRpcServerDiscovery) {
        this.rpcServerDiscovery = zkRegistryRpcServerDiscovery;
    }

    public <T> T getProxy(Class<T> interfaceClass) {
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

            // 创建请求对象，包含类名，方法名，参数类型和实际参数值
            RpcRequest rpcRequest = new RpcRequest(UUID.randomUUID().toString(), method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes(), invocation.getArguments());
            log.debug("RPC request: {}", rpcRequest);
            // 创建client对象，并且发送消息到服务端
            RpcClient rpcClient = new RpcClient(rpcRequest, rpcServerDiscovery);
            RpcResponse rpcResponse = rpcClient.send();
            // 返回调用结果
            return rpcResponse.getResult();
        }
    }
}







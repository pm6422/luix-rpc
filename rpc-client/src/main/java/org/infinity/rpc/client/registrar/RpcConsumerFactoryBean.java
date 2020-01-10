package org.infinity.rpc.client.registrar;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.RpcClient;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.registry.ZkRegistryRpcServerDiscovery;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

@Slf4j
public class RpcConsumerFactoryBean implements InitializingBean, FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

    private final Class<?>                     consumerInterface;
    private       ZkRegistryRpcServerDiscovery zkRegistryRpcServerDiscovery;

    public RpcConsumerFactoryBean(Class<?> consumerInterface) {
        this.consumerInterface = consumerInterface;
    }

    public ZkRegistryRpcServerDiscovery getZkRegistryRpcServerDiscovery() {
        return zkRegistryRpcServerDiscovery;
    }

    public void setZkRegistryRpcServerDiscovery(ZkRegistryRpcServerDiscovery zkRegistryRpcServerDiscovery) {
        this.zkRegistryRpcServerDiscovery = zkRegistryRpcServerDiscovery;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    public Object getProxy() {
        Object instance = Proxy.newProxyInstance(consumerInterface.getClassLoader(), new Class<?>[]{consumerInterface}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
                    // TODO: 调查调用的原因
                    log.debug("Invoked Object.toString()");
                    return null;
                }

                // 创建请求对象，包含类名，方法名，参数类型和实际参数值
                RpcRequest rpcRequest = new RpcRequest(UUID.randomUUID().toString(), method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes(), args);
                log.debug("RPC request: {}", rpcRequest);
                // 创建client对象，并且发送消息到服务端
                RpcClient rpcClient = new RpcClient(rpcRequest, zkRegistryRpcServerDiscovery);
                RpcResponse rpcResponse = rpcClient.send();
                // 返回调用结果
                return rpcResponse.getResult();
            }
        });
        //返回一个代理对象
        return instance;
    }

    @Override
    public Object getObject() throws Exception {
        return this.getProxy();
    }

    @Override
    public Class<?> getObjectType() {
        return consumerInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {

    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

    }
}

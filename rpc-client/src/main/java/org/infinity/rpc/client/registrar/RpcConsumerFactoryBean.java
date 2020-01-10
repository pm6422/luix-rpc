package org.infinity.rpc.client.registrar;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.infinity.rpc.client.RpcClient;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.registry.ZkRegistryRpcServerDiscovery;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
public class RpcConsumerFactoryBean implements FactoryBean<Object>, BeanFactoryAware, InitializingBean {

    private final Class<?>                     consumerInterface;
    private       BeanFactory                  beanFactory;
    private       ZkRegistryRpcServerDiscovery rpcServerDiscovery;

    public RpcConsumerFactoryBean(Class<?> consumerInterface) {
        Assert.notNull(consumerInterface, "Consumer interface must not be null!");
        this.consumerInterface = consumerInterface;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        rpcServerDiscovery = this.beanFactory.getBean(ZkRegistryRpcServerDiscovery.class);
        Assert.notNull(rpcServerDiscovery, "RPC server discovery bean must be created!");
    }

    public Object getProxy() {
        ProxyFactory result = new ProxyFactory();
        result.setInterfaces(consumerInterface);
        result.addAdvice(new MethodInvokingMethodInterceptor());
        return result.getProxy(consumerInterface.getClassLoader());
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

    class MethodInvokingMethodInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method method = invocation.getMethod();
            if (method.getDeclaringClass() == Object.class && method.getName().equals("toString")) {
                // TODO: 调查调用的原因
                log.debug("Invoked Object.toString()");
                return null;
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


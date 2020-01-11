package org.infinity.rpc.client.registrar;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.RpcConsumerProxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

@Slf4j
public class RpcConsumerFactoryBean<T> implements FactoryBean<T>, BeanFactoryAware, InitializingBean {

    private final Class<T>         consumerInterface;
    private       BeanFactory      beanFactory;
    private       RpcConsumerProxy rpcConsumerProxy;

    public RpcConsumerFactoryBean(Class<T> consumerInterface) {
        Assert.notNull(consumerInterface, "Consumer interface must not be null!");
        this.consumerInterface = consumerInterface;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        rpcConsumerProxy = this.beanFactory.getBean(RpcConsumerProxy.class);
        Assert.notNull(rpcConsumerProxy, "RPC client proxy bean must be initialized first!");
    }

    @Override
    public T getObject() throws Exception {
        return rpcConsumerProxy.getProxy(consumerInterface);
    }

    @Override
    public Class<T> getObjectType() {
        return consumerInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}


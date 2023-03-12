package com.luixtech.rpc.spring.boot.starter.startup;

import com.luixtech.rpc.spring.boot.starter.config.LuixRpcProperties;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * The spring application listener used to start and stop the RPC application.
 */
public class RpcLifecycleApplicationListener extends ExecuteOnceApplicationListener implements Ordered, BeanFactoryAware {

    @Resource
    private       LuixRpcProperties luixRpcProperties;
    private final RpcLifecycle      rpcLifecycle;
    /**
     * {@link DefaultListableBeanFactory} can register bean definition
     */
    private       DefaultListableBeanFactory beanFactory;

    public RpcLifecycleApplicationListener() {
        this.rpcLifecycle = RpcLifecycle.getInstance();
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory,
                "It requires an instance of ".concat(DefaultListableBeanFactory.class.getSimpleName()));
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @PostConstruct
    public void init() {
        Validate.notNull(luixRpcProperties, "luixProperties must NOT be null!");
    }

    @Override
    protected void onApplicationContextEvent(ApplicationContextEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            onContextRefreshedEvent((ContextRefreshedEvent) event);
        } else if (event instanceof ContextClosedEvent) {
            // ContextClosedEvent will be triggered while encountering startup error but no throwing any exception
            onContextClosedEvent((ContextClosedEvent) event);
        }
    }

    private void onContextRefreshedEvent(ContextRefreshedEvent event) {
        rpcLifecycle.start(beanFactory, luixRpcProperties);
    }

    private void onContextClosedEvent(ContextClosedEvent event) {
        rpcLifecycle.destroy(luixRpcProperties);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}

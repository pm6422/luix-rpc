package org.infinity.rpc.core.config.spring;

import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

import javax.annotation.PostConstruct;

/**
 * The spring application listener used to start and stop the RPC application.
 */
public class RpcLifecycleApplicationListener extends ExecuteOnceApplicationListener implements Ordered {

    @Autowired
    private       InfinityProperties rpcProperties;
    private final RpcLifecycle       rpcLifecycle;

    public RpcLifecycleApplicationListener() {
        this.rpcLifecycle = RpcLifecycle.getInstance();
    }

    @PostConstruct
    public void init() {
        Validate.notNull(rpcProperties, "RPC properties must NOT be null!");
    }

    @Override
    protected void onApplicationContextEvent(ApplicationContextEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            onContextRefreshedEvent((ContextRefreshedEvent) event);
        } else if (event instanceof ContextClosedEvent) {
            onContextClosedEvent((ContextClosedEvent) event);
        }
    }

    private void onContextRefreshedEvent(ContextRefreshedEvent event) {
        rpcLifecycle.start(rpcProperties);
    }

    private void onContextClosedEvent(ContextClosedEvent event) {
        rpcLifecycle.stop(rpcProperties);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}

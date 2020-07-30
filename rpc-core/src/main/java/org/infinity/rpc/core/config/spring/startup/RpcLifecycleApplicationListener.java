package org.infinity.rpc.core.config.spring.startup;

import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.registry.RegistryConfig;
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
    private       InfinityProperties infinityProperties;
    @Autowired
    private       RegistryConfig     registryConfig;
    private final RpcLifecycle       rpcLifecycle;

    public RpcLifecycleApplicationListener() {
        this.rpcLifecycle = RpcLifecycle.getInstance();
    }

    @PostConstruct
    public void init() {
        Validate.notNull(infinityProperties, "Infinity properties must NOT be null!");
    }

    @Override
    protected void onApplicationContextEvent(ApplicationContextEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            onContextRefreshedEvent((ContextRefreshedEvent) event);
        } else if (event instanceof ContextClosedEvent) {
            // ContextClosedEvent will be triggered while encountering startup error but no any exception thrown
            onContextClosedEvent((ContextClosedEvent) event);
        }
    }

    private void onContextRefreshedEvent(ContextRefreshedEvent event) {
        rpcLifecycle.start(infinityProperties, registryConfig.getRegistryUrls());
    }

    private void onContextClosedEvent(ContextClosedEvent event) {
        rpcLifecycle.destroy(infinityProperties, registryConfig.getRegistryUrls());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}

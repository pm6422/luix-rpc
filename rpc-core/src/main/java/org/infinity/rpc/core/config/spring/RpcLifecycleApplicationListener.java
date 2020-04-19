package org.infinity.rpc.core.config.spring;

import org.infinity.rpc.core.server.RpcApplicationLifecycle;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

/**
 * The spring application listener used to start and shutdown the RPC application.
 */
public class RpcLifecycleApplicationListener extends ExecuteOnceApplicationListener implements Ordered {

    private final RpcApplicationLifecycle rpcApplicationLifecycle;

    public RpcLifecycleApplicationListener() {
        this.rpcApplicationLifecycle = RpcApplicationLifecycle.getInstance();
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
        rpcApplicationLifecycle.start();
    }

    private void onContextClosedEvent(ContextClosedEvent event) {
        rpcApplicationLifecycle.stop();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

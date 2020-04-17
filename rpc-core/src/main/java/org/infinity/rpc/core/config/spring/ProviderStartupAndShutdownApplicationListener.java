package org.infinity.rpc.core.config.spring;

import org.infinity.rpc.core.config.spring.server.RpcServerLauncher;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

/**
 * The spring application listener used to start and shutdown the RPC provider.
 */
public class ProviderStartupAndShutdownApplicationListener extends ExecuteOnceApplicationListener implements Ordered {

    private final RpcServerLauncher rpcServerLauncher;

    public ProviderStartupAndShutdownApplicationListener() {
        this.rpcServerLauncher = RpcServerLauncher.getInstance();
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
        rpcServerLauncher.start();
    }

    private void onContextClosedEvent(ContextClosedEvent event) {
        rpcServerLauncher.stop();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

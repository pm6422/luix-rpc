package org.infinity.rpc.core.config.spring;

import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.core.Ordered;

/**
 * The spring application listener which is used to startup and shutdown the RPC server.
 */
public class ProviderStartupAndShutdownApplicationListener extends ExecuteOnceApplicationListener implements Ordered {
    @Override
    protected void onApplicationContextEvent(ApplicationContextEvent event) {

    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

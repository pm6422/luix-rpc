package org.infinity.luix.core.exchange.checkhealth;

import org.infinity.luix.core.constant.ProviderConstants;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.server.messagehandler.MessageHandler;
import org.infinity.luix.utilities.serviceloader.ServiceLoader;
import org.infinity.luix.utilities.serviceloader.annotation.Spi;
import org.infinity.luix.utilities.serviceloader.annotation.SpiScope;

import java.util.Optional;

@Spi(scope = SpiScope.SINGLETON)
public interface HealthChecker {

    /**
     * Create health check request object
     *
     * @return request object
     */
    Requestable createRequest();

    /**
     * Wrap message handler in order to support check health
     *
     * @param handler message handler
     * @return wrapped message handler
     */
    MessageHandler wrapMessageHandler(MessageHandler handler);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static HealthChecker getInstance(String name) {
        return Optional.ofNullable(ServiceLoader.forClass(HealthChecker.class).load(name))
                .orElseThrow(() -> new RpcFrameworkException("No health checker [" + name + "] found!"));
    }

    /**
     * Get instance associated with the specified name
     *
     * @param providerUrl provider url
     * @return instance
     */
    static HealthChecker getInstance(Url providerUrl) {
        final String name = providerUrl.getOption(ProviderConstants.HEALTH_CHECKER, ProviderConstants.HEALTH_CHECKER_VAL_DEFAULT);
        return Optional.ofNullable(ServiceLoader.forClass(HealthChecker.class).load(name))
                .orElseThrow(() -> new RpcFrameworkException("No health checker [" + name + "] found!"));
    }
}

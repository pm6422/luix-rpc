package org.infinity.rpc.core.exchange.checkhealth;

import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.serviceloader.ServiceLoader;
import org.infinity.rpc.utilities.serviceloader.annotation.Spi;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiScope;

import java.util.Optional;

import static org.infinity.rpc.core.constant.ServiceConstants.HEALTH_CHECKER;
import static org.infinity.rpc.core.constant.ServiceConstants.HEALTH_CHECKER_VAL_DEFAULT;

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
        final String name = providerUrl.getOption(HEALTH_CHECKER, HEALTH_CHECKER_VAL_DEFAULT);
        return Optional.ofNullable(ServiceLoader.forClass(HealthChecker.class).load(name))
                .orElseThrow(() -> new RpcFrameworkException("No health checker [" + name + "] found!"));
    }
}

package com.luixtech.rpc.core.exchange.checkhealth;

import com.luixtech.rpc.core.constant.ProviderConstants;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.rpc.core.server.handler.InvocationHandleable;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import com.luixtech.utilities.serviceloader.annotation.Spi;
import com.luixtech.utilities.serviceloader.annotation.SpiScope;

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
    InvocationHandleable wrap(InvocationHandleable handler);

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
        final String name = providerUrl.getOption(ProviderConstants.HEALTH_CHECKER, ProviderConstants.HEALTH_CHECKER_VAL_V1);
        return Optional.ofNullable(ServiceLoader.forClass(HealthChecker.class).load(name))
                .orElseThrow(() -> new RpcFrameworkException("No health checker [" + name + "] found!"));
    }
}

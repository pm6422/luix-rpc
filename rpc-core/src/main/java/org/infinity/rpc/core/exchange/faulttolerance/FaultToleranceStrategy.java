package org.infinity.rpc.core.exchange.faulttolerance;

import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

/**
 * Fault tolerance refers to the ability of a system to continue operating without interruption
 * when one or more of its components fail.
 *
 * @param <T>: The interface class of the provider
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface FaultToleranceStrategy<T> {

    // TODO: check use
    void setClientUrl(Url clientUrl);

    // TODO: check use
    Url getClientUrl();

    Responseable call(LoadBalancer<T> loadBalancer, Requestable request);

    /**
     * Get fault tolerance strategy instance associated with the specified name
     *
     * @param name specified fault tolerance strategy name
     * @return fault tolerance strategy instance
     */
    static FaultToleranceStrategy getInstance(String name) {
        return ServiceLoader.forClass(FaultToleranceStrategy.class).load(name);
    }
}
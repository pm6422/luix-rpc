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

    /**
     * Set client url
     *
     * @param clientUrl client url
     */
    void setClientUrl(Url clientUrl);

    /**
     * Get client url
     *
     * @return client url
     */
    Url getClientUrl();

    /**
     * Call the RPC
     *
     * @param loadBalancer load balancer
     * @param request      RPC request
     * @return RPC response
     */
    Responseable call(LoadBalancer<T> loadBalancer, Requestable request);

    /**
     * Get fault tolerance strategy instance associated with the specified name
     *
     * @param name specified fault tolerance strategy name
     * @return fault tolerance strategy instance
     */
    @SuppressWarnings("unchecked")
    static <T> FaultToleranceStrategy<T> getInstance(String name) {
        return ServiceLoader.forClass(FaultToleranceStrategy.class).load(name);
    }
}
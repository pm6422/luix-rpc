package org.infinity.rpc.core.client.faulttolerance;

import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

/**
 * Fault tolerance refers to the ability of a system to continue operating without interruption
 * when one or more of its components fail.
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface FaultTolerance {

    /**
     * Set consumer url
     *
     * @param consumerUrl consumer url
     */
    void setConsumerUrl(Url consumerUrl);

    /**
     * Get consumer url
     *
     * @return consumer url
     */
    Url getConsumerUrl();

    /**
     * Set load balancer
     *
     * @param loadBalancer load balancer
     */
    void setLoadBalancer(LoadBalancer loadBalancer);

    /**
     * Get load balancer
     *
     * @return load balancer
     */
    LoadBalancer getLoadBalancer();

    /**
     * Invoke the RPC provider
     *
     * @param request RPC request
     * @return RPC response
     */
    Responseable invoke(Requestable request);

    /**
     * Destroy
     */
    void destroy();

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static FaultTolerance getInstance(String name) {
        return ServiceLoader.forClass(FaultTolerance.class).load(name);
    }
}
package com.luixtech.rpc.core.client.faulttolerance;

import com.luixtech.rpc.core.client.loadbalancer.LoadBalancer;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.exception.impl.RpcConfigException;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import com.luixtech.utilities.serviceloader.annotation.Spi;
import com.luixtech.utilities.serviceloader.annotation.SpiScope;

import java.util.Optional;

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
     * Invoke the RPC provider at client side
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
        return Optional.ofNullable(ServiceLoader.forClass(FaultTolerance.class).load(name))
                .orElseThrow(() -> new RpcConfigException("Fault tolerance [" + name + "] does NOT exist, " +
                        "please check whether the correct dependency is in your class path!"));
    }
}
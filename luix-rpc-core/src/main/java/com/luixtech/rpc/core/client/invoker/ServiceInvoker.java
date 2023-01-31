package com.luixtech.rpc.core.client.invoker;

import com.luixtech.rpc.core.client.faulttolerance.FaultTolerance;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import com.luixtech.utilities.serviceloader.annotation.Spi;
import com.luixtech.utilities.serviceloader.annotation.SpiScope;

/**
 * A service invoker used to invoke service provider at client side
 * One invoker for one protocol
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface ServiceInvoker {
    /**
     * Create a service provider invoker instance
     *
     * @param interfaceName      interface name
     * @param faultToleranceName fault tolerance name
     * @param loadBalancerName   load balancer name
     * @param consumerUrl        consumer url
     */
    void init(String interfaceName,
              String faultToleranceName,
              String loadBalancerName,
              Url consumerUrl);

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable invoke(Requestable request);

    /**
     * Check whether it is available
     *
     * @return {@code true} if it was active and {@code false} otherwise
     */
    boolean isActive();

    /**
     * Set provider interface name
     *
     * @param interfaceName interface name
     */
    void setInterfaceName(String interfaceName);

    /**
     * Set fault tolerance
     *
     * @param faultTolerance fault tolerance
     */
    void setFaultTolerance(FaultTolerance faultTolerance);

    /**
     * Get fault tolerance
     *
     * @return faultTolerance fault tolerance
     */
    FaultTolerance getFaultTolerance();

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
    static ServiceInvoker getInstance(String name) {
        return ServiceLoader.forClass(ServiceInvoker.class).load(name);
    }
}

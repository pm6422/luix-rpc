package org.infinity.rpc.core.client.cluster;

import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

/**
 * One cluster for one protocol, only one node of a cluster can handle the request
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface InvokerCluster {
    /**
     * Initialize
     */
    void init();

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
    static InvokerCluster getInstance(String name) {
        return ServiceLoader.forClass(InvokerCluster.class).load(name);
    }

    /**
     * Create a provider invoker cluster
     *
     * @param clusterName        provider invoker cluster name
     * @param interfaceName      interface name
     * @param faultToleranceName fault tolerance name
     * @param loadBalancerName   load balancer name
     * @param clientUrl          client url
     * @return provider invoker cluster
     */
    static InvokerCluster createCluster(String clusterName,
                                        String interfaceName,
                                        String faultToleranceName,
                                        String loadBalancerName,
                                        Url clientUrl) {
        // It support one cluster for one protocol for now, but do not support one cluster for one provider
        InvokerCluster invokerCluster = InvokerCluster.getInstance(clusterName);
        invokerCluster.setInterfaceName(interfaceName);
        FaultTolerance faultTolerance = FaultTolerance.getInstance(faultToleranceName);
        faultTolerance.setClientUrl(clientUrl);
        faultTolerance.setLoadBalancer(LoadBalancer.getInstance(loadBalancerName));
        invokerCluster.setFaultTolerance(faultTolerance);
        // Initialize
        invokerCluster.init();
        return invokerCluster;
    }
}

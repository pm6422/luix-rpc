package org.infinity.rpc.core.client.cluster;

import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.Invokable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.util.List;

/**
 * One cluster for one protocol, only one node of a cluster can handle the request
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface ProviderCluster {
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
    Responseable call(Requestable request);

    /**
     * Check whether it is available
     *
     * @return true: available, false: unavailable
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
     * Set load balancer
     *
     * @param loadBalancer client load balancer
     */
    void setLoadBalancer(LoadBalancer loadBalancer);

    /**
     * Refresh provider callers when providers is online or offline
     *
     * @param importers provider call
     */
    void refresh(List<Invokable> importers);

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
    static ProviderCluster getInstance(String name) {
        return ServiceLoader.forClass(ProviderCluster.class).load(name);
    }

    /**
     * Create a provider cluster
     *
     * @param clusterName        provider cluster name
     * @param interfaceName      interface name
     * @param faultToleranceName fault tolerance name
     * @param loadBalancerName   load balancer name
     * @param clientUrl          client url
     * @return provider cluster
     */
    static ProviderCluster createCluster(String clusterName,
                                         String interfaceName,
                                         String faultToleranceName,
                                         String loadBalancerName,
                                         Url clientUrl) {
        // It support one cluster for one protocol for now, but do not support one cluster for one provider
        ProviderCluster providerCluster = ProviderCluster.getInstance(clusterName);
        providerCluster.setInterfaceName(interfaceName);
        FaultTolerance faultTolerance = FaultTolerance.getInstance(faultToleranceName);
        faultTolerance.setClientUrl(clientUrl);
        providerCluster.setFaultTolerance(faultTolerance);
        LoadBalancer loadBalancer = LoadBalancer.getInstance(loadBalancerName);
        providerCluster.setLoadBalancer(loadBalancer);
        // Initialize
        providerCluster.init();
        return providerCluster;
    }
}

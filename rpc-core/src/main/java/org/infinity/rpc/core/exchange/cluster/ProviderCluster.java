package org.infinity.rpc.core.exchange.cluster;

import org.infinity.rpc.core.exchange.ProviderCallable;
import org.infinity.rpc.core.exchange.faulttolerance.FaultToleranceStrategy;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.util.List;

/**
 * One cluster for one protocol, only one server node under a cluster can receive the request
 *
 * @param <T>: The interface class of the provider
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface ProviderCluster<T> extends ProviderCallable<T> {

    void setProtocol(String protocol);

    String getProtocol();

    void setRegistryInfo(RegistryInfo registryInfo);

    void setLoadBalancer(LoadBalancer<T> loadBalancer);

    LoadBalancer<T> getLoadBalancer();

    void setFaultToleranceStrategy(FaultToleranceStrategy<T> faultToleranceStrategy);

    FaultToleranceStrategy<T> getFaultToleranceStrategy();

    List<ProviderCaller<T>> getProviderCallers();

    /**
     * Refresh provider callers when providers is online or offline
     *
     * @param providerCallers provider call
     */
    void refresh(List<ProviderCaller<T>> providerCallers);

    /**
     * Get provider cluster instance associated with the specified name
     *
     * @param name specified provider cluster name
     * @return provider cluster instance
     */
    static ProviderCluster getInstance(String name) {
        return ServiceLoader.forClass(ProviderCluster.class).load(name);
    }

    /**
     * Create a provider cluster
     * todo: remove clientUrl param
     *
     * @param protocol           protocol name
     * @param clusterName        provider cluster name
     * @param loadBalancerName   load balancer name
     * @param faultToleranceName fault tolerance name
     * @param <T>                provider interface class
     * @return provider cluster
     */
    @SuppressWarnings({"unchecked"})
    static <T> ProviderCluster<T> createCluster(String protocol, String clusterName, String loadBalancerName, String faultToleranceName) {
        // It support one cluster for one protocol for now, but do not support one cluster for one provider
        ProviderCluster<T> providerCluster = ProviderCluster.getInstance(clusterName);
        LoadBalancer<T> loadBalancer = LoadBalancer.getInstance(loadBalancerName);
        FaultToleranceStrategy<T> faultTolerance = FaultToleranceStrategy.getInstance(faultToleranceName);

        providerCluster.setProtocol(protocol);
        providerCluster.setFaultToleranceStrategy(faultTolerance);
        providerCluster.setLoadBalancer(loadBalancer);
        // Initialize
        providerCluster.init();

        // Add to cluster holder
        ProviderClusterHolder.getInstance().addCluster(clusterName, providerCluster);

        return providerCluster;
    }
}

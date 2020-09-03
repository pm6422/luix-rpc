package org.infinity.rpc.core.exchange.cluster;

import org.infinity.rpc.core.exchange.ProviderCallable;
import org.infinity.rpc.core.exchange.ha.ClusterHighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

/**
 * One cluster for one protocol, only one server node under a cluster can receive the request
 *
 * @param <T>: The interface class of the provider
 */
@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface ProviderCluster<T> extends ProviderCallable<T> {
    /**
     * @param registryInfo
     */
    void setRegistryInfo(RegistryInfo registryInfo);

    /**
     * Refresh requesters when online or offline
     *
     * @param providerRequesters
     */
    void onRefresh(List<ProviderRequester<T>> providerRequesters);

    void setLoadBalancer(LoadBalancer<T> loadBalancer);

    LoadBalancer<T> getLoadBalancer();

    void setHighAvailability(ClusterHighAvailability<T> clusterHighAvailability);

    ClusterHighAvailability<T> getHighAvailability();

    List<ProviderRequester<T>> getRequesters();

    // todo: remove clientUrl param
    static <T> ProviderCluster<T> createCluster(String clusterName, String loadBalancerName, String haName, Url clientUrl) {
        ProviderCluster<T> providerCluster = ServiceInstanceLoader.getServiceLoader(ProviderCluster.class).load(clusterName);
        LoadBalancer<T> loadBalancer = ServiceInstanceLoader.getServiceLoader(LoadBalancer.class).load(loadBalancerName);
        ClusterHighAvailability<T> ha = ServiceInstanceLoader.getServiceLoader(ClusterHighAvailability.class).load(haName);
        ha.setClientUrl(clientUrl);

        providerCluster.setLoadBalancer(loadBalancer);
        providerCluster.setHighAvailability(ha);
        // Initialize
        providerCluster.init();

        // Add to cluster holder
        ClusterHolder.getInstance().addCluster(clusterName, providerCluster);

        return providerCluster;
    }
}

package org.infinity.rpc.core.exchange.cluster;

import org.infinity.rpc.core.exchange.RpcCallable;
import org.infinity.rpc.core.exchange.ha.HighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface Cluster<T> extends RpcCallable<T> {
    /**
     * @param registryInfo
     */
    void setRegistryInfo(RegistryInfo registryInfo);

    /**
     * Refresh requesters when online or offline
     *
     * @param requesters
     */
    void onRefresh(List<Requester<T>> requesters);

    void setLoadBalancer(LoadBalancer<T> loadBalancer);

    LoadBalancer<T> getLoadBalancer();

    void setHighAvailability(HighAvailability<T> highAvailability);

    HighAvailability<T> getHighAvailability();

    List<Requester<T>> getRequesters();
}

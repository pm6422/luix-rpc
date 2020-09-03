package org.infinity.rpc.core.exchange.loadbalancer;

import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

/**
 * {@link org.infinity.rpc.core.exchange.ha.ClusterHighAvailability} select providers via load balance algorithm.
 *
 * @param <T>: The interface class of the provider
 */
@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface LoadBalancer<T> {
    /**
     * Refresh requesters when online or offline
     *
     * @param providerRequesters new discovered provider requesters
     */
    void onRefresh(List<ProviderRequester<T>> providerRequesters);

    /**
     * Select provider node via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected provider requester
     */
    ProviderRequester<T> selectProviderNode(Requestable request);

    /**
     * Select multiple provider nodes via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected provider requesters
     */
    List<ProviderRequester<T>> selectProviderNodes(Requestable request);

//
//    void setWeightString(String weightString);
}

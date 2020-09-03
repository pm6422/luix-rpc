package org.infinity.rpc.core.exchange.loadbalancer;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

/**
 *
 * @param <T>: The interface class of the provider
 */
@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface LoadBalancer<T> {
    /**
     * Refresh requesters when online or offline
     *
     * @param providerRequesters
     */
    void onRefresh(List<ProviderRequester<T>> providerRequesters);

    /**
     * @param request
     * @return
     */
    ProviderRequester<T> selectNode(Requestable request);

    /**
     * @param request
     * @return
     */
    List<ProviderRequester<T>> selectNodes(Requestable request);

//
//    void setWeightString(String weightString);


}

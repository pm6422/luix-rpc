package org.infinity.rpc.core.exchange.loadbalancer;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface LoadBalancer<T> {
    /**
     * Refresh requesters when online or offline
     *
     * @param requesters
     */
    void onRefresh(List<Requester<T>> requesters);

    /**
     * @param request
     * @return
     */
    Requester<T> selectNode(Requestable request);

    /**
     * @param request
     * @return
     */
    List<Requester<T>> selectNodes(Requestable request);

//
//    void setWeightString(String weightString);


}

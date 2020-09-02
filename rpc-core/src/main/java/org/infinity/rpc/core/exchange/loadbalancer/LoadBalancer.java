package org.infinity.rpc.core.exchange.loadbalancer;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.ProtocolRequester;
import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

@Spi(scope = ServiceInstanceScope.PROTOTYPE)
public interface LoadBalancer<T> {
    /**
     * Refresh requesters when online or offline
     *
     * @param protocolRequesters
     */
    void onRefresh(List<ProtocolRequester<T>> protocolRequesters);

    /**
     * @param request
     * @return
     */
    ProtocolRequester<T> selectNode(Requestable request);

    /**
     * @param request
     * @return
     */
    List<ProtocolRequester<T>> selectNodes(Requestable request);

//
//    void setWeightString(String weightString);


}

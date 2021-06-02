package org.infinity.rpc.core.client.loadbalancer;

import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.utilities.serviceloader.ServiceLoader;
import org.infinity.rpc.utilities.serviceloader.annotation.Spi;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiScope;

import java.util.List;

/**
 * {@link FaultTolerance} select providers via load balance algorithm.
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface LoadBalancer {
    /**
     * Get provider invokers
     *
     * @return provider invokers
     */
    List<Sendable> getInvokers();

    /**
     * Refresh provider invokers after providers become active or inactive
     *
     * @param invokers new discovered provider invokers
     */
    void refresh(List<Sendable> invokers);

    /**
     * Select provider node via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected provider invoker
     */
    Sendable selectProviderNode(Requestable request);

    /**
     * Select multiple provider nodes via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected provider invokers
     */
    List<Sendable> selectProviderNodes(Requestable request);

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    @SuppressWarnings("unchecked")
    static LoadBalancer getInstance(String name) {
        return ServiceLoader.forClass(LoadBalancer.class).load(name);
    }

    /**
     * Destroy
     */
    void destroy();

//
//    void setWeightString(String weightString);
}

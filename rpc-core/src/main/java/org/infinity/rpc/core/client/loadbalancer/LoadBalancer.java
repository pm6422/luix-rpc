package org.infinity.rpc.core.client.loadbalancer;

import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.request.ProviderCaller;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.util.List;

/**
 * {@link FaultTolerance} select providers via load balance algorithm.
 *
 * @param <T>: The interface class of the provider
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface LoadBalancer<T> {
    /**
     * Get provider callers
     *
     * @return provider callers
     */
    List<ProviderCaller<T>> getProviderCallers();

    /**
     * Refresh provider callers when online or offline
     *
     * @param providerCallers new discovered provider callers
     */
    void refresh(List<ProviderCaller<T>> providerCallers);

    /**
     * Select provider node via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected provider caller
     */
    ProviderCaller<T> selectProviderNode(Requestable request);

    /**
     * Select multiple provider nodes via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected provider callers
     */
    List<ProviderCaller<T>> selectProviderNodes(Requestable request);

    /**
     * Get load balancer instance associated with the specified name
     *
     * @param name specified load balancer name
     * @return load balancer instance
     */
    @SuppressWarnings("unchecked")
    static <T> LoadBalancer<T> getInstance(String name) {
        return ServiceLoader.forClass(LoadBalancer.class).load(name);
    }

    /**
     * Destroy
     */
    void destroy();

//
//    void setWeightString(String weightString);
}

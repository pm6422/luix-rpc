package com.luixtech.rpc.core.client.loadbalancer;

import com.luixtech.rpc.core.client.faulttolerance.FaultTolerance;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.client.sender.Sendable;
import com.luixtech.rpc.core.exception.impl.RpcConfigException;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import com.luixtech.utilities.serviceloader.annotation.Spi;
import com.luixtech.utilities.serviceloader.annotation.SpiScope;

import java.util.List;
import java.util.Optional;

/**
 * {@link FaultTolerance} select providers via load balance algorithm.
 */
@Spi(scope = SpiScope.PROTOTYPE)
public interface LoadBalancer {
    /**
     * Get RPC request senders
     *
     * @return RPC request senders
     */
    List<Sendable> getRequestSenders();

    /**
     * Refresh RPC request senders after providers become active or inactive
     *
     * @param requestSender new discovered RPC request senders
     */
    void refresh(List<Sendable> requestSender);

    /**
     * Select one active RPC request sender via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected RPC request sender
     */
    Sendable selectActiveSender(Requestable request);

    /**
     * Select all active RPC request senders with a specified order via load balance algorithm
     *
     * @param request RPC request instance
     * @return selected RPC request senders
     */
    List<Sendable> selectAllActiveSenders(Requestable request);

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
    static LoadBalancer getInstance(String name) {
        return Optional.ofNullable(ServiceLoader.forClass(LoadBalancer.class).load(name))
                .orElseThrow(() -> new RpcConfigException("Fault tolerance [" + name + "] does NOT exist, " +
                        "please check whether the correct dependency is in your class path!"));
    }
}

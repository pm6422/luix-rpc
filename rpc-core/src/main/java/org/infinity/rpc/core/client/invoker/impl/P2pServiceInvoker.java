package org.infinity.rpc.core.client.invoker.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.invoker.ServiceInvoker;
import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.INVOKER_VAL_P2P;

/**
 * Only one service provider can process one request
 * todo: ClusterSpi
 */
@Slf4j
@SpiName(INVOKER_VAL_P2P)
@Setter
@Getter
public class P2pServiceInvoker implements ServiceInvoker {
    private boolean        active = false;
    private String         interfaceName;
    private FaultTolerance faultTolerance;

    @Override
    public ServiceInvoker createInstance(String interfaceName, String faultToleranceName,
                                         String loadBalancerName, Url consumerUrl) {
        this.setInterfaceName(interfaceName);
        FaultTolerance faultTolerance = FaultTolerance.getInstance(faultToleranceName);
        faultTolerance.setLoadBalancer(LoadBalancer.getInstance(loadBalancerName));
        faultTolerance.setConsumerUrl(consumerUrl);
        this.setFaultTolerance(faultTolerance);
        // Initialize
        this.init();
        return this;
    }

    @Override
    public void init() {
        active = true;
        ShutdownHook.add(this::destroy);
    }

    @Override
    public void destroy() {
        active = false;
        faultTolerance.destroy();
    }

    /**
     * Call chain:
     * Cluster fault tolerance strategy =>
     * LB select node =>
     * RPC sender
     *
     * @param request request object
     * @return response
     */
    @Override
    public Responseable invoke(Requestable request) {
        if (!active) {
            throw new RpcFrameworkException("No active service [" + this + "] found!");
        }
        return faultTolerance.invoke(request);
    }

    @Override
    public String toString() {
        if (StringUtils.isEmpty(interfaceName)) {
            return P2pServiceInvoker.class.getSimpleName();
        }
        return P2pServiceInvoker.class.getSimpleName().concat(":").concat(interfaceName);
    }
}

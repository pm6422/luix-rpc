package org.infinity.luix.core.client.invoker.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.client.loadbalancer.LoadBalancer;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.constant.ConsumerConstants;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.client.faulttolerance.FaultTolerance;
import org.infinity.luix.core.client.invoker.ServiceInvoker;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.utilities.destory.ShutdownHook;
import org.infinity.luix.utilities.serviceloader.annotation.SpiName;

/**
 * Only one service provider can process one request
 * todo: ClusterSpi
 */
@Slf4j
@SpiName(ConsumerConstants.INVOKER_VAL_DEFAULT)
@Setter
@Getter
public class DefaultServiceInvoker implements ServiceInvoker {
    private boolean        active = false;
    private String         interfaceName;
    private FaultTolerance faultTolerance;

    @Override
    public void init(String interfaceName, String faultToleranceName,
                     String loadBalancerName, Url consumerUrl) {
        this.setInterfaceName(interfaceName);
        FaultTolerance faultTolerance = FaultTolerance.getInstance(faultToleranceName);
        faultTolerance.setLoadBalancer(LoadBalancer.getInstance(loadBalancerName));
        faultTolerance.setConsumerUrl(consumerUrl);
        this.setFaultTolerance(faultTolerance);
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
            return DefaultServiceInvoker.class.getSimpleName();
        }
        return DefaultServiceInvoker.class.getSimpleName().concat(":").concat(interfaceName);
    }
}

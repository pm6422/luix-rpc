package org.infinity.rpc.core.client.cluster.impl;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.cluster.InvokerCluster;
import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.client.request.Invokable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.util.List;

import static org.infinity.rpc.core.constant.ConsumerConstants.CLUSTER_VAL_DEFAULT;
import static org.infinity.rpc.core.constant.ProtocolConstants.THROW_EXCEPTION;
import static org.infinity.rpc.core.constant.ProtocolConstants.THROW_EXCEPTION_VAL_DEFAULT;

/**
 * todo: ClusterSpi
 */
@Slf4j
@SpiName(CLUSTER_VAL_DEFAULT)
@Setter
public class DefaultInvokerCluster implements InvokerCluster {
    private boolean        active = false;
    private String         interfaceName;
    private FaultTolerance faultTolerance;
    private LoadBalancer   loadBalancer;

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void init() {
        active = true;
        ShutdownHook.add(() -> destroy());
    }

    /**
     * Update new provider callers of load balancer
     *
     * @param newImporters new provider callers
     */
    @Override
    public synchronized void refresh(List<Invokable> newImporters) {
        if (CollectionUtils.isEmpty(newImporters)) {
            return;
        }
        // Set new provider callers to load balancer
        loadBalancer.refresh(newImporters);
    }

    @Override
    public void destroy() {
        active = false;
        loadBalancer.destroy();
    }

    @Override
    public Responseable call(Requestable request) {
        if (active) {
            try {
                return faultTolerance.call(request, loadBalancer);
            } catch (Exception e) {
                return handleError(request, e);
            }
        }
        return handleError(request, new RpcServiceException(RpcErrorMsgConstant.SERVICE_NOT_FOUND));
    }

    private Responseable handleError(Requestable request, Exception cause) {
        if (ExceptionUtils.isBizException(cause)) {
            // Throw the exception if it is business one
            throw (RuntimeException) cause;
        }

        boolean parameter = faultTolerance.getClientUrl().getBooleanOption(THROW_EXCEPTION, THROW_EXCEPTION_VAL_DEFAULT);
        if (parameter) {
            if (cause instanceof RpcAbstractException) {
                throw (RpcAbstractException) cause;
            } else {
                throw new RpcServiceException("Failed to call the request!", cause);
            }
        }
        return RpcResponse.error(request, cause);
    }

    @Override
    public String toString() {
        if (StringUtils.isEmpty(interfaceName)) {
            return DefaultInvokerCluster.class.getSimpleName();
        }
        return DefaultInvokerCluster.class.getSimpleName().concat(":").concat(interfaceName);
    }
}

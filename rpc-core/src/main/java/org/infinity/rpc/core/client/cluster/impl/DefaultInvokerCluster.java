package org.infinity.rpc.core.client.cluster.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.cluster.InvokerCluster;
import org.infinity.rpc.core.client.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.impl.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.impl.RpcServiceException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.CLUSTER_VAL_DEFAULT;
import static org.infinity.rpc.core.constant.ProtocolConstants.THROW_EXCEPTION;
import static org.infinity.rpc.core.constant.ProtocolConstants.THROW_EXCEPTION_VAL_DEFAULT;

/**
 * todo: ClusterSpi
 */
@Slf4j
@SpiName(CLUSTER_VAL_DEFAULT)
@Setter
@Getter
public class DefaultInvokerCluster implements InvokerCluster {
    private boolean        active = false;
    private String         interfaceName;
    private FaultTolerance faultTolerance;

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

    @Override
    public Responseable invoke(Requestable request) {
        if (active) {
            try {
                return faultTolerance.invoke(request);
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

        boolean parameter = faultTolerance.getConsumerUrl().getBooleanOption(THROW_EXCEPTION, THROW_EXCEPTION_VAL_DEFAULT);
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

package org.infinity.rpc.core.exchange.cluster.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.faulttolerance.FaultTolerance;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcResponse;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.List;

/**
 * todo: ClusterSpi
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@ServiceName("default")
public class DefaultProviderCluster<T> implements ProviderCluster<T> {
    private boolean           active = false;
    private Class<T>          interfaceClass;
    private String            protocol;
    private FaultTolerance<T> faultTolerance;
    private LoadBalancer<T>   loadBalancer;

    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    @Override
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setLoadBalancer(@NonNull LoadBalancer<T> loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public LoadBalancer<T> getLoadBalancer() {
        return loadBalancer;
    }

    @Override
    public void setFaultTolerance(@NonNull FaultTolerance<T> faultTolerance) {
        this.faultTolerance = faultTolerance;
    }

    @Override
    public FaultTolerance<T> getFaultTolerance() {
        return faultTolerance;
    }

    @Override
    public void init() {
        active = true;
        ShutdownHook.add(() -> destroy());
    }

    /**
     * Update new provider callers of load balancer
     *
     * @param newProviderCallers new provider callers
     */
    @Override
    public synchronized void refresh(List<ProviderCaller<T>> newProviderCallers) {
        if (CollectionUtils.isEmpty(newProviderCallers)) {
            return;
        }
        // Set new provider callers to load balancer
        loadBalancer.refresh(newProviderCallers);
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
                return faultTolerance.call(loadBalancer, request);
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

        boolean parameter = faultTolerance.getClientUrl()
                .getBooleanOption(Url.PARAM_THROW_EXCEPTION, Url.PARAM_THROW_EXCEPTION_DEFAULT_VALUE);
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
        if (interfaceClass == null) {
            return DefaultProviderCluster.class.getSimpleName();
        }
        return DefaultProviderCluster.class.getSimpleName().concat(":").concat(interfaceClass.getName());
    }
}

package org.infinity.rpc.core.exchange.cluster.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.destroy.ScheduledDestroyThreadPool;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.faulttolerance.FaultToleranceStrategy;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcResponse;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.core.url.UrlParam;
import org.infinity.rpc.core.utils.ExceptionUtils;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.infinity.rpc.core.destroy.ScheduledDestroyThreadPool.DESTROY_REQUESTER_THREAD_POOL;

/**
 * @param <T>: The interface class of the provider
 */
@Slf4j
@ServiceName("default")
public class DefaultProviderCluster<T> implements ProviderCluster<T> {
    private static final int                        DELAY_TIME = 1000;
    private RegistryInfo              registryInfo;
    private FaultToleranceStrategy<T> faultToleranceStrategy;
    private LoadBalancer<T>           loadBalancer;
    private              List<ProviderRequester<T>> providerRequesters;
    private final        AtomicBoolean              available  = new AtomicBoolean(false);

    @Override
    public void setRegistryInfo(@NonNull RegistryInfo registryInfo) {
        this.registryInfo = registryInfo;
    }

    @Override
    public Class<T> getInterfaceClass() {
        return CollectionUtils.isEmpty(providerRequesters) ? null : providerRequesters.get(0).getInterfaceClass();
    }

    @Override
    public boolean isAvailable() {
        return available.get();
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
    public void setFaultToleranceStrategy(@NonNull FaultToleranceStrategy<T> faultToleranceStrategy) {
        this.faultToleranceStrategy = faultToleranceStrategy;
    }

    @Override
    public FaultToleranceStrategy<T> getFaultToleranceStrategy() {
        return faultToleranceStrategy;
    }

    @Override
    public List<ProviderRequester<T>> getRequesters() {
        return providerRequesters;
    }

    @Override
    public void init() {
        onRefresh(providerRequesters);
        available.set(true);
    }

    @Override
    public synchronized void onRefresh(List<ProviderRequester<T>> providerRequesters) {
        if (CollectionUtils.isEmpty(providerRequesters)) {
            return;
        }

        loadBalancer.onRefresh(providerRequesters);

        List<ProviderRequester<T>> oldProviderRequesters = this.providerRequesters;
        this.providerRequesters = providerRequesters;

        if (CollectionUtils.isEmpty(oldProviderRequesters)) {
            return;
        }

        List<ProviderRequester<T>> delayDestroyProviderRequesters = new ArrayList<>();
        for (ProviderRequester<T> oldProviderRequester : oldProviderRequesters) {
            if (providerRequesters.contains(oldProviderRequester)) {
                continue;
            }

            // Destroy the old requester if old requester is useless
            delayDestroyProviderRequesters.add(oldProviderRequester);
        }

        if (CollectionUtils.isNotEmpty(delayDestroyProviderRequesters)) {
            ScheduledDestroyThreadPool.scheduleDelayTask(DESTROY_REQUESTER_THREAD_POOL, DELAY_TIME, TimeUnit.MILLISECONDS, () -> {
                for (ProviderRequester<?> providerRequester : delayDestroyProviderRequesters) {
                    try {
                        providerRequester.destroy();
                        log.info("Destroyed the requester with url: {}", providerRequester.getProviderUrl().getUri());
                    } catch (Exception e) {
                        log.error(MessageFormat.format("Failed to destroy the requester with url: {0}", providerRequester.getProviderUrl().getUri()), e);
                    }
                }
            });
        }
    }

    @Override
    public void destroy() {
        available.set(false);
        for (ProviderRequester<T> providerRequester : this.providerRequesters) {
            providerRequester.destroy();
        }
    }

    @Override
    public Responseable call(Requestable request) {
        if (available.get()) {
            try {
                return faultToleranceStrategy.call(loadBalancer, request);
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

        if (Boolean.parseBoolean(faultToleranceStrategy.getClientUrl().getParameter(UrlParam.throwException.getName(), UrlParam.throwException.getValue()))) {
            if (cause instanceof RpcAbstractException) {
                throw (RpcAbstractException) cause;
            } else {
                RpcServiceException ex = new RpcServiceException("Failed to call the request!", cause);
                throw ex;
            }
        }
        return RpcResponse.error(request, cause);
    }
}

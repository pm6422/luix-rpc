package org.infinity.rpc.core.exchange.cluster.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.destroy.ScheduledDestroyThreadPool;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.ha.HighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.ProtocolRequester;
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
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
@ServiceName("default")
public class DefaultCluster<T> implements Cluster<T> {
    private static final int                        DELAY_TIME = 1000;
    private              RegistryInfo               registryInfo;
    private              HighAvailability<T>        highAvailability;
    private              LoadBalancer<T>            loadBalancer;
    private              List<ProtocolRequester<T>> protocolRequesters;
    private final        AtomicBoolean              available  = new AtomicBoolean(false);

    @Override
    public void setRegistryInfo(@NonNull RegistryInfo registryInfo) {
        this.registryInfo = registryInfo;
    }

    @Override
    public Class<T> getInterfaceClass() {
        return CollectionUtils.isEmpty(protocolRequesters) ? null : protocolRequesters.get(0).getInterfaceClass();
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
    public void setHighAvailability(@NonNull HighAvailability<T> highAvailability) {
        this.highAvailability = highAvailability;
    }

    @Override
    public HighAvailability<T> getHighAvailability() {
        return highAvailability;
    }

    @Override
    public List<ProtocolRequester<T>> getRequesters() {
        return protocolRequesters;
    }

    @Override
    public void init() {
        onRefresh(protocolRequesters);
        available.set(true);
    }

    @Override
    public synchronized void onRefresh(List<ProtocolRequester<T>> protocolRequesters) {
        if (CollectionUtils.isEmpty(protocolRequesters)) {
            return;
        }

        loadBalancer.onRefresh(protocolRequesters);

        List<ProtocolRequester<T>> oldProtocolRequesters = this.protocolRequesters;
        this.protocolRequesters = protocolRequesters;

        if (CollectionUtils.isEmpty(oldProtocolRequesters)) {
            return;
        }

        List<ProtocolRequester<T>> delayDestroyProtocolRequesters = new ArrayList<>();
        for (ProtocolRequester<T> oldProtocolRequester : oldProtocolRequesters) {
            if (protocolRequesters.contains(oldProtocolRequester)) {
                continue;
            }

            // Destroy the old requester if old requester is useless
            delayDestroyProtocolRequesters.add(oldProtocolRequester);
        }

        if (CollectionUtils.isNotEmpty(delayDestroyProtocolRequesters)) {
            ScheduledDestroyThreadPool.scheduleDelayTask(DESTROY_REQUESTER_THREAD_POOL, DELAY_TIME, TimeUnit.MILLISECONDS, () -> {
                for (ProtocolRequester<?> protocolRequester : delayDestroyProtocolRequesters) {
                    try {
                        protocolRequester.destroy();
                        log.info("Destroyed the requester with url: {}", protocolRequester.getProviderUrl().getUri());
                    } catch (Exception e) {
                        log.error(MessageFormat.format("Failed to destroy the requester with url: {0}", protocolRequester.getProviderUrl().getUri()), e);
                    }
                }
            });
        }
    }

    @Override
    public void destroy() {
        available.set(false);
        for (ProtocolRequester<T> protocolRequester : this.protocolRequesters) {
            protocolRequester.destroy();
        }
    }

    @Override
    public Responseable call(Requestable request) {
        if (available.get()) {
            try {
                return highAvailability.call(request, loadBalancer);
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

        if (Boolean.parseBoolean(highAvailability.getClientUrl().getParameter(UrlParam.throwException.getName(), UrlParam.throwException.getValue()))) {
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

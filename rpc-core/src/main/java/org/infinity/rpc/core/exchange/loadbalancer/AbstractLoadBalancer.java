package org.infinity.rpc.core.exchange.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.RpcInvocationException;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.exchange.request.Requestable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.infinity.rpc.core.destroy.ScheduledThreadPool.DESTROY_CALLER_THREAD_POOL;

/**
 * @param <T>: The interface class of the provider
 */
@Slf4j
public abstract class AbstractLoadBalancer<T> implements LoadBalancer<T> {
    private static final int                     DELAY_TIME = 1000;
    protected            List<ProviderCaller<T>> providerCallers;

    @Override
    public void refresh(List<ProviderCaller<T>> newProviderCallers) {
        List<ProviderCaller<T>> oldProviderCallers = providerCallers;
        // Assign new ones to provider callers
        providerCallers = newProviderCallers;

        if (CollectionUtils.isEmpty(oldProviderCallers)) {
            return;
        }

        Collection<ProviderCaller<T>> inactiveOnes = CollectionUtils.subtract(newProviderCallers, oldProviderCallers);
        if (CollectionUtils.isEmpty(inactiveOnes)) {
            return;
        }
        // Destroy the inactive provider callers
        destroyInactiveProviderCallers(inactiveOnes);
    }

    @Override
    public ProviderCaller<T> selectProviderNode(Requestable request) {
        if (CollectionUtils.isEmpty(this.providerCallers)) {
            throw new RpcInvocationException("No available provider caller for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }

        // Make a copy for thread safe purpose
        List<ProviderCaller<T>> providerCallers = new ArrayList<>(this.providerCallers);
        ProviderCaller<T> providerCaller = null;
        if (providerCallers.size() > 1) {
            providerCaller = doSelectNode(request);
        } else if (providerCallers.size() == 1 && providerCallers.get(0).isActive()) {
            providerCaller = providerCallers.get(0);
        }
        if (providerCaller == null) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active provider caller for now, " +
                    "please check whether the server is ok!");
        }
        return providerCaller;
    }

    @Override
    public List<ProviderCaller<T>> selectProviderNodes(Requestable request) {
        if (CollectionUtils.isEmpty(this.providerCallers)) {
            throw new RpcInvocationException("No active provider caller for now, " +
                    "please check whether the server is ok!");
        }
        // Make a copy for thread safe purpose
        List<ProviderCaller<T>> providerCallers = new ArrayList<>(this.providerCallers);
        List<ProviderCaller<T>> selected = new ArrayList<>();
        if (providerCallers.size() > 1) {
            selected = doSelectNodes(request);
        } else if (providerCallers.size() == 1 && providerCallers.get(0).isActive()) {
            selected.add(providerCallers.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active provider caller for now, " +
                    "please check whether the server is ok!");
        }
        return selected;
    }

    @Override
    public List<ProviderCaller<T>> getProviderCallers() {
        return providerCallers;
    }

    private void destroyInactiveProviderCallers(Collection<ProviderCaller<T>> delayDestroyProviderCallers) {
        // Execute once after a daley time
        ScheduledThreadPool.scheduleDelayTask(DESTROY_CALLER_THREAD_POOL, DELAY_TIME, TimeUnit.MILLISECONDS, () -> {
            for (ProviderCaller<?> providerCaller : delayDestroyProviderCallers) {
                try {
                    providerCaller.destroy();
                    log.info("Destroyed the caller with url: {}", providerCaller.getProviderUrl().getUri());
                } catch (Exception e) {
                    log.error(MessageFormat.format("Failed to destroy the caller with url: {0}", providerCaller.getProviderUrl().getUri()), e);
                }
            }
        });
    }

    @Override
    public void destroy() {
        providerCallers.forEach(ProviderCaller::destroy);
    }

    /**
     * Select one provider node
     *
     * @param request request instance
     * @return selected provider caller
     */
    protected abstract ProviderCaller<T> doSelectNode(Requestable request);

    /**
     * Select multiple provider nodes
     *
     * @param request request instance
     * @return selected provider callers
     */
    protected abstract List<ProviderCaller<T>> doSelectNodes(Requestable request);
}

package org.infinity.rpc.core.client.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.impl.RpcInvocationException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.infinity.rpc.core.destroy.ScheduledThreadPool.DESTROY_INVOKER_THREAD_POOL;

/**
 *
 */
@Slf4j
public abstract class AbstractLoadBalancer implements LoadBalancer {
    private static final int            DELAY_TIME = 1000;
    protected            List<Sendable> invokers;

    /**
     * Update new provider invokers
     *
     * @param invokers new provider invokers
     */
    @Override
    public synchronized void refresh(List<Sendable> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }
        List<Sendable> oldInvokers = this.invokers;
        // Assign new ones to provider invokers
        this.invokers = invokers;

        if (CollectionUtils.isEmpty(oldInvokers)) {
            return;
        }

        Collection<Sendable> inactiveOnes = CollectionUtils.subtract(invokers, oldInvokers);
        if (CollectionUtils.isEmpty(inactiveOnes)) {
            return;
        }
        // Destroy the inactive provider invokers
        destroyInactiveInvokers(inactiveOnes);
    }

    @Override
    public Sendable selectProviderNode(Requestable request) {
        if (CollectionUtils.isEmpty(this.invokers)) {
            throw new RpcInvocationException("No available provider invoker for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }

        // Make a copy for thread safe purpose
        List<Sendable> invokers = new ArrayList<>(this.invokers);
        Sendable invoker = null;
        if (invokers.size() > 1) {
            invoker = doSelectNode(request);
        } else if (invokers.size() == 1 && invokers.get(0).isActive()) {
            invoker = invokers.get(0);
        }
        if (invoker == null) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active provider invoker for now, " +
                    "please check whether the server is ok!");
        }
        return invoker;
    }

    @Override
    public List<Sendable> selectProviderNodes(Requestable request) {
        if (CollectionUtils.isEmpty(this.invokers)) {
            throw new RpcInvocationException("No active provider invoker for now, " +
                    "please check whether the server is ok!");
        }
        // Make a copy for thread safe purpose
        List<Sendable> invokers = new ArrayList<>(this.invokers);
        List<Sendable> selected = new ArrayList<>();
        if (invokers.size() > 1) {
            selected = doSelectNodes(request);
        } else if (invokers.size() == 1 && invokers.get(0).isActive()) {
            selected.add(invokers.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active provider invoker for now, " +
                    "please check whether the server is ok!");
        }
        return selected;
    }

    @Override
    public List<Sendable> getInvokers() {
        return invokers;
    }

    private void destroyInactiveInvokers(Collection<Sendable> delayDestroyInvokers) {
        // Execute once after a daley time
        ScheduledThreadPool.scheduleDelayTask(DESTROY_INVOKER_THREAD_POOL, DELAY_TIME, TimeUnit.MILLISECONDS, () -> {
            for (Sendable invoker : delayDestroyInvokers) {
                try {
                    invoker.destroy();
                    log.info("Destroyed the caller with url: {}", invoker.getProviderUrl().getUri());
                } catch (Exception e) {
                    log.error(MessageFormat.format("Failed to destroy the caller with url: {0}", invoker.getProviderUrl().getUri()), e);
                }
            }
        });
    }

    @Override
    public void destroy() {
        if (CollectionUtils.isNotEmpty(invokers)) {
            invokers.forEach(Sendable::destroy);
        }
    }

    /**
     * Select one provider node
     *
     * @param request request instance
     * @return selected provider invoker
     */
    protected abstract Sendable doSelectNode(Requestable request);

    /**
     * Select multiple provider nodes
     *
     * @param request request instance
     * @return selected provider invokers
     */
    protected abstract List<Sendable> doSelectNodes(Requestable request);
}

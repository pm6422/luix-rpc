package org.infinity.rpc.core.client.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.impl.RpcInvocationException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.infinity.rpc.core.destroy.ScheduledThreadPool.DESTROY_SENDER_THREAD_POOL;

/**
 *
 */
@Slf4j
public abstract class AbstractLoadBalancer implements LoadBalancer {
    private static final int            DESTROY_INTERVAL = 1000;
    protected            List<Sendable> requestSenders;

    /**
     * Update RPC request senders
     *
     * @param newSenders new provider invokers
     */
    @Override
    public synchronized void refresh(List<Sendable> newSenders) {
        if (CollectionUtils.isEmpty(newSenders)) {
            return;
        }
        List<Sendable> oldSenders = this.requestSenders;
        // Assign new values
        this.requestSenders = newSenders;
        if (CollectionUtils.isEmpty(oldSenders)) {
            return;
        }
        Collection<Sendable> inactiveOnes = CollectionUtils.subtract(newSenders, oldSenders);
        if (CollectionUtils.isEmpty(inactiveOnes)) {
            return;
        }
        // Destroy the inactive provider invokers
        destroyInactiveInvokers(inactiveOnes);
    }

    @Override
    public Sendable selectSender(Requestable request) {
        if (CollectionUtils.isEmpty(this.requestSenders)) {
            throw new RpcInvocationException("No available provider invoker for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }

        // Make a copy for thread safe purpose
        List<Sendable> invokers = new ArrayList<>(this.requestSenders);
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
    public List<Sendable> selectSenders(Requestable request) {
        if (CollectionUtils.isEmpty(this.requestSenders)) {
            throw new RpcInvocationException("No active provider invoker for now, " +
                    "please check whether the server is ok!");
        }
        // Make a copy for thread safe purpose
        List<Sendable> invokers = new ArrayList<>(this.requestSenders);
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
    public List<Sendable> getRequestSenders() {
        return requestSenders;
    }

    private void destroyInactiveInvokers(Collection<Sendable> senders) {
        // Execute once after a delay time
        ScheduledThreadPool.scheduleDelayTask(DESTROY_SENDER_THREAD_POOL, DESTROY_INTERVAL, TimeUnit.MILLISECONDS, () -> {
            for (Sendable sender : senders) {
                try {
                    sender.destroy();
                    log.info("Destroyed the RPC request sender for provider url {}", sender.getProviderUrl().getUri());
                } catch (Exception e) {
                    log.error(MessageFormat.format("Failed to destroy the RPC request sender for url: {0}", sender.getProviderUrl().getUri()), e);
                }
            }
        });
    }

    @Override
    public void destroy() {
        if (CollectionUtils.isNotEmpty(requestSenders)) {
            requestSenders.forEach(Sendable::destroy);
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

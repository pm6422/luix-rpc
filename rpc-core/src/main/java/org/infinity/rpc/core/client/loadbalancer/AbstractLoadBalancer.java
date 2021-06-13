package org.infinity.rpc.core.client.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.exception.impl.RpcInvocationException;
import org.infinity.rpc.core.thread.ScheduledThreadPool;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.infinity.rpc.core.thread.ScheduledThreadPool.DESTROY_SENDER_DELAY;
import static org.infinity.rpc.core.thread.ScheduledThreadPool.DESTROY_SENDER_THREAD_POOL;

/**
 *
 */
@Slf4j
public abstract class AbstractLoadBalancer implements LoadBalancer {
    protected List<Sendable> requestSenders;

    /**
     * Update RPC request senders
     *
     * @param newSenders new RPC request senders
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
        // Destroy the inactive RPC request senders
        destroyInactiveSenders(inactiveOnes);
    }

    @Override
    public Sendable selectActiveSender(Requestable request) {
        if (CollectionUtils.isEmpty(this.requestSenders)) {
            throw new RpcInvocationException("No active RPC request sender, " +
                    "please check whether the server is ok!");
        }
        // Make a copy for thread safe purpose
        List<Sendable> senders = new ArrayList<>(this.requestSenders);
        Sendable sender = null;
        if (senders.size() > 1) {
            sender = doSelectSender(request);
        } else if (senders.size() == 1 && senders.get(0).isActive()) {
            sender = senders.get(0);
        }
        if (sender == null) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active RPC request sender, " +
                    "please check whether the server is ok!");
        }
        return sender;
    }

    @Override
    public List<Sendable> selectAllActiveSenders(Requestable request) {
        if (CollectionUtils.isEmpty(this.requestSenders)) {
            throw new RpcInvocationException("No active RPC request sender, " +
                    "please check the status of associated service provider!");
        }
        // Make a copy for thread safe purpose
        List<Sendable> senders = new ArrayList<>(this.requestSenders);
        List<Sendable> selected = new ArrayList<>();
        if (senders.size() > 1) {
            selected = doSelectSenders(request);
        } else if (senders.size() == 1 && senders.get(0).isActive()) {
            selected.add(senders.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No active RPC request sender, " +
                    "please check whether the server is ok!");
        }
        return selected;
    }

    @Override
    public List<Sendable> getRequestSenders() {
        return requestSenders;
    }

    private void destroyInactiveSenders(Collection<Sendable> senders) {
        // Execute once after a delay time
        ScheduledThreadPool.scheduleDelayTask(DESTROY_SENDER_THREAD_POOL, DESTROY_SENDER_DELAY, () -> {
            for (Sendable sender : senders) {
                try {
                    sender.destroy();
                } catch (Exception e) {
                    log.error(MessageFormat.format("Failed to destroy the RPC request sender for url {0}",
                            sender.getProviderUrl().getUri()), e);
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
     * Select one RPC request sender
     *
     * @param request request instance
     * @return selected RPC request sender
     */
    protected abstract Sendable doSelectSender(Requestable request);

    /**
     * Select multiple RPC request senders
     *
     * @param request request instance
     * @return selected RPC request senders
     */
    protected abstract List<Sendable> doSelectSenders(Requestable request);
}

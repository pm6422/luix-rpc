package org.infinity.rpc.core.exchange.loadbalancer;

import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exception.RpcInvocationException;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.exchange.request.Requestable;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <T>: The interface class of the provider
 */
public abstract class AbstractLoadBalancer<T> implements LoadBalancer<T> {
    protected List<ProviderCaller<T>> providerCallers;

    @Override
    public void refresh(List<ProviderCaller<T>> providerCallers) {
        this.providerCallers = providerCallers;
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

    public List<ProviderCaller<T>> getProviderCallers() {
        return providerCallers;
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

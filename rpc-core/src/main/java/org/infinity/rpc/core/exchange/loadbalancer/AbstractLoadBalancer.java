package org.infinity.rpc.core.exchange.loadbalancer;

import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exception.RpcInvocationException;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.exchange.request.Requestable;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <T>: The interface class of the provider
 */
public abstract class AbstractLoadBalancer<T> implements LoadBalancer<T> {
    protected List<ProviderRequester<T>> providerRequesters;

    @Override
    public void refresh(List<ProviderRequester<T>> providerRequesters) {
        this.providerRequesters = providerRequesters;
    }

    @Override
    public ProviderRequester<T> selectProviderNode(Requestable request) {
        if (CollectionUtils.isEmpty(this.providerRequesters)) {
            throw new RpcInvocationException("No available provider requester for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }

        // Make a copy for thread safe purpose
        List<ProviderRequester<T>> providerRequesters = new ArrayList<>(this.providerRequesters);
        ProviderRequester<T> providerRequester = null;
        if (providerRequesters.size() > 1) {
            providerRequester = doSelectNode(request);
        } else if (providerRequesters.size() == 1 && providerRequesters.get(0).isAvailable()) {
            providerRequester = providerRequesters.get(0);
        }
        if (providerRequester == null) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No available provider requester for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }
        return providerRequester;
    }

    @Override
    public List<ProviderRequester<T>> selectProviderNodes(Requestable request) {
        if (CollectionUtils.isEmpty(this.providerRequesters)) {
            throw new RpcInvocationException("No available provider requester for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }
        // Make a copy for thread safe purpose
        List<ProviderRequester<T>> providerRequesters = new ArrayList<>(this.providerRequesters);
        List<ProviderRequester<T>> selected = new ArrayList<>();
        if (providerRequesters.size() > 1) {
            selected = doSelectNodes(request);
        } else if (providerRequesters.size() == 1 && providerRequesters.get(0).isAvailable()) {
            selected.add(providerRequesters.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // Provider may be lost when executing doSelect
            throw new RpcInvocationException("No available provider requester for RPC call for now! " +
                    "Please check whether there are available providers now!");
        }
        return selected;
    }

    public List<ProviderRequester<T>> getRequesters() {
        return providerRequesters;
    }

    /**
     * Select one node
     *
     * @param request request instance
     * @return selected provider requester
     */
    protected abstract ProviderRequester<T> doSelectNode(Requestable request);

    /**
     * Select multiple nodes
     *
     * @param request request instance
     * @return selected provider requesters
     */
    protected abstract List<ProviderRequester<T>> doSelectNodes(Requestable request);
}

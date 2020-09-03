package org.infinity.rpc.core.exchange.loadbalancer;

import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exception.RpcInvocationException;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.ProviderRequester;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @param <T>: The interface class of the provider
 */
public abstract class AbstractLoadBalancer<T> implements LoadBalancer<T> {
    protected List<ProviderRequester<T>> providerRequesters;

    @Override
    public void onRefresh(List<ProviderRequester<T>> providerRequesters) {
        this.providerRequesters = providerRequesters;
    }

    @Override
    public ProviderRequester<T> selectNode(Requestable request) {
        if (CollectionUtils.isEmpty(this.providerRequesters)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
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
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        return providerRequester;
    }

    @Override
    public List<ProviderRequester<T>> selectNodes(Requestable request) {
        if (CollectionUtils.isEmpty(this.providerRequesters)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        // Make a copy for thread safe purpose
        List<ProviderRequester<T>> providerRequesters = new ArrayList<>(this.providerRequesters);

        List<ProviderRequester<T>> selected = new ArrayList<>();

        if (CollectionUtils.isEmpty(providerRequesters)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        if (providerRequesters.size() > 1) {
            selected = doSelectNodes(request);
        } else if (providerRequesters.size() == 1 && providerRequesters.get(0).isAvailable()) {
            selected.add(providerRequesters.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        return selected;
    }

    public List<ProviderRequester<T>> getRequesters() {
        return providerRequesters;
    }

    protected abstract ProviderRequester<T> doSelectNode(Requestable request);

    protected abstract List<ProviderRequester<T>> doSelectNodes(Requestable request);
}

package org.infinity.rpc.core.exchange.loadbalance;

import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exception.RpcInvocationException;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLoadBalancer<T> implements LoadBalancer<T> {
    protected List<Requester<T>> requesters;

    @Override
    public void onRefresh(List<Requester<T>> requesters) {
        this.requesters = requesters;
    }

    @Override
    public Requester<T> selectNode(Requestable request) {
        // Make a copy for thread safe purpose
        List<Requester<T>> requesters = new ArrayList<>(this.requesters);

        if (CollectionUtils.isEmpty(requesters)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }

        Requester<T> requester = null;
        if (requesters.size() > 1) {
            requester = doSelectNode(request);
        } else if (requesters.size() == 1 && requesters.get(0).isAvailable()) {
            requester = requesters.get(0);
        }
        if (requester == null) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        return requester;
    }

    @Override
    public List<Requester<T>> selectNodes(Requestable request) {
        List<Requester<T>> selected = new ArrayList<>();

        // Make a copy for thread safe purpose
        List<Requester<T>> requesters = new ArrayList<>(this.requesters);

        if (CollectionUtils.isEmpty(requesters)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        if (requesters.size() > 1) {
            selected = doSelectNodes(request);
        } else if (requesters.size() == 1 && requesters.get(0).isAvailable()) {
            selected.add(requesters.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        return selected;
    }

    public List<Requester<T>> getRequesters() {
        return requesters;
    }

    protected abstract Requester<T> doSelectNode(Requestable request);

    protected abstract List<Requester<T>> doSelectNodes(Requestable request);
}

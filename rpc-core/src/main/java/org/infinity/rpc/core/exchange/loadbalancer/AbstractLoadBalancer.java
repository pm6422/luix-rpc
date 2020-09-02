package org.infinity.rpc.core.exchange.loadbalancer;

import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exception.RpcInvocationException;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.ProtocolRequester;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLoadBalancer<T> implements LoadBalancer<T> {
    protected List<ProtocolRequester<T>> protocolRequesters;

    @Override
    public void onRefresh(List<ProtocolRequester<T>> protocolRequesters) {
        this.protocolRequesters = protocolRequesters;
    }

    @Override
    public ProtocolRequester<T> selectNode(Requestable request) {
        if (CollectionUtils.isEmpty(this.protocolRequesters)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }

        // Make a copy for thread safe purpose
        List<ProtocolRequester<T>> protocolRequesters = new ArrayList<>(this.protocolRequesters);

        ProtocolRequester<T> protocolRequester = null;
        if (protocolRequesters.size() > 1) {
            protocolRequester = doSelectNode(request);
        } else if (protocolRequesters.size() == 1 && protocolRequesters.get(0).isAvailable()) {
            protocolRequester = protocolRequesters.get(0);
        }
        if (protocolRequester == null) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        return protocolRequester;
    }

    @Override
    public List<ProtocolRequester<T>> selectNodes(Requestable request) {
        if (CollectionUtils.isEmpty(this.protocolRequesters)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        // Make a copy for thread safe purpose
        List<ProtocolRequester<T>> protocolRequesters = new ArrayList<>(this.protocolRequesters);

        List<ProtocolRequester<T>> selected = new ArrayList<>();

        if (CollectionUtils.isEmpty(protocolRequesters)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        if (protocolRequesters.size() > 1) {
            selected = doSelectNodes(request);
        } else if (protocolRequesters.size() == 1 && protocolRequesters.get(0).isAvailable()) {
            selected.add(protocolRequesters.get(0));
        }
        if (CollectionUtils.isEmpty(selected)) {
            // TODO: change log
            throw new RpcInvocationException("No available requester for RPC call for now!");
        }
        return selected;
    }

    public List<ProtocolRequester<T>> getRequesters() {
        return protocolRequesters;
    }

    protected abstract ProtocolRequester<T> doSelectNode(Requestable request);

    protected abstract List<ProtocolRequester<T>> doSelectNodes(Requestable request);
}

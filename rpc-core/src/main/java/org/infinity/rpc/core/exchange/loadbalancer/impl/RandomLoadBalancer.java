package org.infinity.rpc.core.exchange.loadbalancer.impl;

import org.infinity.rpc.core.exchange.loadbalancer.AbstractLoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.ProtocolRequester;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ServiceName("random")
public class RandomLoadBalancer<T> extends AbstractLoadBalancer<T> {

    @Override
    protected ProtocolRequester doSelectNode(Requestable request) {
        int index = getIndex(protocolRequesters);
        for (int i = 0; i < protocolRequesters.size(); i++) {
            ProtocolRequester<T> protocolRequester = protocolRequesters.get((i + index) % protocolRequesters.size());
            if (protocolRequester.isAvailable()) {
                return protocolRequester;
            }
        }
        return null;
    }

    @Override
    protected List<ProtocolRequester<T>> doSelectNodes(Requestable request) {
        List<ProtocolRequester<T>> selected = new ArrayList<>();

        int index = getIndex(protocolRequesters);
        for (int i = 0; i < protocolRequesters.size(); i++) {
            ProtocolRequester<T> protocolRequester = protocolRequesters.get((i + index) % protocolRequesters.size());
            if (protocolRequester.isAvailable()) {
                selected.add(protocolRequester);
            }
        }
        return selected;
    }

    private int getIndex(List<ProtocolRequester<T>> protocolRequesters) {
        return (int) (ThreadLocalRandom.current().nextDouble() * protocolRequesters.size());
    }
}

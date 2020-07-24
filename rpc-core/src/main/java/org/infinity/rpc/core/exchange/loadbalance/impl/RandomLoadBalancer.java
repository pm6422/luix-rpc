package org.infinity.rpc.core.exchange.loadbalance.impl;

import org.infinity.rpc.core.exchange.loadbalance.AbstractLoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ServiceName("random")
public class RandomLoadBalancer<T> extends AbstractLoadBalancer<T> {

    @Override
    protected Requester doSelectOne(Requestable request) {
        int index = getIndex(requesters);
        for (int i = 0; i < requesters.size(); i++) {
            Requester<T> requester = requesters.get((i + index) % requesters.size());
            if (requester.isAvailable()) {
                return requester;
            }
        }
        return null;
    }

    @Override
    protected List<Requester<T>> doSelect(Requestable request) {
        List<Requester<T>> selected = new ArrayList<>();

        int index = getIndex(requesters);
        for (int i = 0; i < requesters.size(); i++) {
            Requester<T> requester = requesters.get((i + index) % requesters.size());
            if (requester.isAvailable()) {
                selected.add(requester);
            }
        }
        return selected;
    }

    private int getIndex(List<Requester<T>> requesters) {
        return (int) (ThreadLocalRandom.current().nextDouble() * requesters.size());
    }
}

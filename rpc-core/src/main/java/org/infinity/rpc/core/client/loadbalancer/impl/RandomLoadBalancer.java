package org.infinity.rpc.core.client.loadbalancer.impl;

import org.infinity.rpc.core.client.loadbalancer.AbstractLoadBalancer;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.infinity.rpc.core.constant.ConsumerConstants.LOAD_BALANCER_VAL_RANDOM;

/**
 *
 */
@SpiName(LOAD_BALANCER_VAL_RANDOM)
public class RandomLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected Sendable doSelectNode(Requestable request) {
        int index = getIndex(requestSenders);
        for (int i = 0; i < requestSenders.size(); i++) {
            Sendable invoker = requestSenders.get((i + index) % requestSenders.size());
            if (invoker.isActive()) {
                return invoker;
            }
        }
        return null;
    }

    @Override
    protected List<Sendable> doSelectNodes(Requestable request) {
        List<Sendable> selected = new ArrayList<>();
        int index = getIndex(requestSenders);
        for (int i = 0; i < requestSenders.size(); i++) {
            Sendable invoker = requestSenders.get((i + index) % requestSenders.size());
            if (invoker.isActive()) {
                selected.add(invoker);
            }
        }
        return selected;
    }

    private int getIndex(List<Sendable> invokers) {
        return (int) (ThreadLocalRandom.current().nextDouble() * invokers.size());
    }
}

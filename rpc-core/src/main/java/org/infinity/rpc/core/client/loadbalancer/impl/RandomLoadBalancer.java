package org.infinity.rpc.core.client.loadbalancer.impl;

import org.infinity.rpc.core.client.loadbalancer.AbstractLoadBalancer;
import org.infinity.rpc.core.client.request.Invokable;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

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
    protected Invokable doSelectNode(Requestable request) {
        int index = getIndex(invokers);
        for (int i = 0; i < invokers.size(); i++) {
            Invokable invoker = invokers.get((i + index) % invokers.size());
            if (invoker.isActive()) {
                return invoker;
            }
        }
        return null;
    }

    @Override
    protected List<Invokable> doSelectNodes(Requestable request) {
        List<Invokable> selected = new ArrayList<>();
        int index = getIndex(invokers);
        for (int i = 0; i < invokers.size(); i++) {
            Invokable invoker = invokers.get((i + index) % invokers.size());
            if (invoker.isActive()) {
                selected.add(invoker);
            }
        }
        return selected;
    }

    private int getIndex(List<Invokable> invokers) {
        return (int) (ThreadLocalRandom.current().nextDouble() * invokers.size());
    }
}

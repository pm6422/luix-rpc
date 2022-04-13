package com.luixtech.rpc.core.client.loadbalancer.impl;

import com.luixtech.rpc.core.client.loadbalancer.AbstractLoadBalancer;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.client.sender.Sendable;
import com.luixtech.utilities.serviceloader.annotation.SpiName;
import com.luixtech.rpc.core.constant.ConsumerConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 */
@SpiName(ConsumerConstants.LOAD_BALANCER_VAL_RANDOM)
public class RandomLoadBalancer extends AbstractLoadBalancer {

    @Override
    protected Sendable doSelectSender(Requestable request) {
        int index = getIndex(requestSenders);
        for (int i = 0; i < requestSenders.size(); i++) {
            Sendable sender = requestSenders.get((i + index) % requestSenders.size());
            if (sender.isActive()) {
                return sender;
            }
        }
        return null;
    }

    @Override
    protected List<Sendable> doSelectSenders(Requestable request) {
        List<Sendable> selected = new ArrayList<>();
        int index = getIndex(requestSenders);
        // Select senders with a random order
        for (int i = 0; i < requestSenders.size(); i++) {
            Sendable sender = requestSenders.get((i + index) % requestSenders.size());
            if (sender.isActive()) {
                selected.add(sender);
            }
        }
        return selected;
    }

    private int getIndex(List<Sendable> senders) {
        return (int) (ThreadLocalRandom.current().nextDouble() * senders.size());
    }
}

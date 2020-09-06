package org.infinity.rpc.core.exchange.loadbalancer.impl;

import org.infinity.rpc.core.exchange.loadbalancer.AbstractLoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @param <T>: The interface class of the provider
 */
@ServiceName("random")
public class RandomLoadBalancer<T> extends AbstractLoadBalancer<T> {

    @Override
    protected ProviderRequester<T> doSelectNode(Requestable request) {
        int index = getIndex(providerRequesters);
        for (int i = 0; i < providerRequesters.size(); i++) {
            ProviderRequester<T> providerRequester = providerRequesters.get((i + index) % providerRequesters.size());
            if (providerRequester.isAvailable()) {
                return providerRequester;
            }
        }
        return null;
    }

    @Override
    protected List<ProviderRequester<T>> doSelectNodes(Requestable request) {
        List<ProviderRequester<T>> selected = new ArrayList<>();
        int index = getIndex(providerRequesters);
        for (int i = 0; i < providerRequesters.size(); i++) {
            ProviderRequester<T> providerRequester = providerRequesters.get((i + index) % providerRequesters.size());
            if (providerRequester.isAvailable()) {
                selected.add(providerRequester);
            }
        }
        return selected;
    }

    private int getIndex(List<ProviderRequester<T>> providerRequesters) {
        return (int) (ThreadLocalRandom.current().nextDouble() * providerRequesters.size());
    }
}

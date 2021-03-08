package org.infinity.rpc.core.client.loadbalancer.impl;

import org.infinity.rpc.core.client.loadbalancer.AbstractLoadBalancer;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.request.Importable;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.infinity.rpc.core.constant.ConsumerConstants.LOAD_BALANCER_VAL_RANDOM;

/**
 *
 * @param <T>: The interface class of the provider
 */
@SpiName(LOAD_BALANCER_VAL_RANDOM)
public class RandomLoadBalancer<T> extends AbstractLoadBalancer<T> {

    @Override
    protected Importable<T> doSelectNode(Requestable request) {
        int index = getIndex(importers);
        for (int i = 0; i < importers.size(); i++) {
            Importable<T> importer = importers.get((i + index) % importers.size());
            if (importer.isActive()) {
                return importer;
            }
        }
        return null;
    }

    @Override
    protected List<Importable<T>> doSelectNodes(Requestable request) {
        List<Importable<T>> selected = new ArrayList<>();
        int index = getIndex(importers);
        for (int i = 0; i < importers.size(); i++) {
            Importable<T> importer = importers.get((i + index) % importers.size());
            if (importer.isActive()) {
                selected.add(importer);
            }
        }
        return selected;
    }

    private int getIndex(List<Importable<T>> importers) {
        return (int) (ThreadLocalRandom.current().nextDouble() * importers.size());
    }
}

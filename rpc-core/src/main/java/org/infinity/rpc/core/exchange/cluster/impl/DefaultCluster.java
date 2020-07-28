package org.infinity.rpc.core.exchange.cluster.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.ha.HighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@ServiceName("default")
public class DefaultCluster<T> implements Cluster<T> {
    private HighAvailability<T> highAvailability;
    private LoadBalancer<T>     loadBalancer;
    private Url                 providerUrl;
    private List<Requester<T>>  requesters;
    private AtomicBoolean       available = new AtomicBoolean(false);

    @Override
    public Class<T> getInterfaceClass() {
        return CollectionUtils.isEmpty(requesters) ? null : requesters.get(0).getInterfaceClass();
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }

    @Override
    public Url getProviderUrl() {
        return providerUrl;
    }

    @Override
    public void setLoadBalancer(LoadBalancer<T> loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public LoadBalancer<T> getLoadBalancer() {
        return loadBalancer;
    }

    @Override
    public void setHighAvailability(HighAvailability<T> highAvailability) {
        this.highAvailability = highAvailability;
    }

    @Override
    public List<Requester<T>> getRequesters() {
        return requesters;
    }

    @Override
    public void init() {
        onRefresh(requesters);
        available.set(true);
    }

    @Override
    public synchronized void onRefresh(List<Requester<T>> requesters) {
        if (CollectionUtils.isEmpty(requesters)) {
            return;
        }

        loadBalancer.onRefresh(requesters);

        List<Requester<T>> oldRequesters = this.requesters;
        this.requesters = requesters;
        highAvailability.setProviderUrl(providerUrl);

        if(CollectionUtils.isEmpty(oldRequesters)) {
            return;
        }

        List<Requester<T>> delayDestroyRequesters = new ArrayList<>();
        for (Requester<T> oldRequester : oldRequesters) {
            if (requesters.contains(oldRequester)) {
                continue;
            }

            // Destroy the old requester if old requester is useless
            delayDestroyRequesters.add(oldRequester);
        }

        if(CollectionUtils.isNotEmpty(delayDestroyRequesters)) {

        }
    }

    @Override
    public void destroy() {
        available.set(false);
        for (Requester<T> requester : this.requesters) {
            requester.destroy();
        }
    }

    @Override
    public Responseable call(Requestable request) {
        return null;
    }
}

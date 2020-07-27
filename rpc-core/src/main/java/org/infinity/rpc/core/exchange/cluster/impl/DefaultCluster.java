package org.infinity.rpc.core.exchange.cluster.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.ha.HighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

import java.util.List;

@Slf4j
@ServiceName("default")
public class DefaultCluster<T> implements Cluster<T> {
    @Override
    public Class<T> getInterfaceClass() {
        return null;
    }

    @Override
    public void setAvailable(boolean available) {

    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Url getProviderUrl() {
        return null;
    }

    @Override
    public Responseable call(Requestable request) {
        return null;
    }

    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }


    @Override
    public void onRefresh(List<Requester<T>> requesters) {

    }

    @Override
    public void setLoadBalancer(LoadBalancer<T> loadBalance) {

    }

    @Override
    public LoadBalancer<T> getLoadBalancer() {
        return null;
    }

    @Override
    public void setHighAvailability(HighAvailability<T> haStrategy) {

    }

    @Override
    public List<Requester<T>> getRequesters() {
        return null;
    }
}

package org.infinity.rpc.core.exchange.cluster.listener;

import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterListener<T> implements ClientListener {
    private       Cluster<T>                   cluster;
    private       List<Url>                    registryUrls;
    private       Url                          clientUrl;
    private       Class<T>                     interfaceClass;
    private final Map<Url, List<Requester<T>>> registryRequestersPerClientUrl = new ConcurrentHashMap<>();

    public ClusterListener(Class<T> interfaceClass, List<Url> registryUrls) {
        this.interfaceClass = interfaceClass;
        this.registryUrls = registryUrls;
    }

    @Override
    public void onSubscribe(Url registryUrl, List<Url> providerUrls) {

        List<Requester<T>> newRequesters = new ArrayList<Requester<T>>();
        for (Url providerUrl : providerUrls) {


        }
    }
}

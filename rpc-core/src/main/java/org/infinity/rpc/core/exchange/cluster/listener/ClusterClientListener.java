package org.infinity.rpc.core.exchange.cluster.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.cluster.ClusterHolder;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@ThreadSafe
public class ClusterClientListener<T> implements ClientListener {
    private       Protocol                     protocol;
    private       List<Url>                    registryUrls;
    private       Url                          clientUrl;
    private       Class<T>                     interfaceClass;
    private final Map<Url, List<Requester<T>>> requestersPerRegistryUrl = new ConcurrentHashMap<>();

    public ClusterClientListener(Class<T> interfaceClass, List<Url> registryUrls, Url clientUrl) {
        this.interfaceClass = interfaceClass;
        this.registryUrls = registryUrls;
        this.clientUrl = clientUrl;
        this.protocol = Protocol.getInstance(clientUrl.getProtocol());
        init();
    }

    private void init() {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            registry.subscribe(clientUrl, this);
        }
    }

    @Override
    public synchronized void onSubscribe(Url registryUrl, List<Url> providerUrls) {
        if (CollectionUtils.isEmpty(providerUrls)) {
            onRegistryEmpty(registryUrl);
            log.warn("ClusterSupport config change notify, urls is empty: registry={} service={} urls=[]", registryUrl.getUri(), clientUrl.getIdentity());
            return;
        }

        List<Requester<T>> newRequesters = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            Requester<T> requester = getExistingRequester(providerUrl, requestersPerRegistryUrl.get(registryUrl));
            if (requester == null) {
                Url providerUrlCopy = providerUrl.copy();
                requester = protocol.createRequester(interfaceClass, providerUrlCopy);
            }
            if (requester != null) {
                newRequesters.add(requester);
            }
        }

        if (CollectionUtils.isEmpty(newRequesters)) {
            onRegistryEmpty(registryUrl);
            return;
        }

        // 此处不销毁requesters，由cluster进行销毁
        requestersPerRegistryUrl.put(registryUrl, newRequesters);
        refreshCluster();
    }

    private Requester<T> getExistingRequester(Url providerUrl, List<Requester<T>> requesters) {
        return CollectionUtils.isEmpty(requesters) ? null :
                requesters
                        .stream()
                        .filter(requester -> Objects.equals(providerUrl, requester.getProviderUrl()))
                        .findFirst()
                        .orElseGet(null);
    }

    private synchronized void onRegistryEmpty(Url excludeRegistryUrl) {
        boolean noMoreOtherRefers = requestersPerRegistryUrl.size() == 1 && requestersPerRegistryUrl.containsKey(excludeRegistryUrl);
        if (noMoreOtherRefers) {
            log.warn(String.format("Ignore notify for no more requesters on this cluster, registry: %s, cluster=%s", excludeRegistryUrl, clientUrl));
        } else {
            requestersPerRegistryUrl.remove(excludeRegistryUrl);
            refreshCluster();
        }
    }

    private synchronized void refreshCluster() {
        List<Requester<T>> allRequesters = requestersPerRegistryUrl.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Loop all the cluster and update requesters
        List<Cluster<T>> clusters = ClusterHolder.getInstance().getClusters();
        clusters.forEach(c -> c.onRefresh(allRequesters));
    }
}

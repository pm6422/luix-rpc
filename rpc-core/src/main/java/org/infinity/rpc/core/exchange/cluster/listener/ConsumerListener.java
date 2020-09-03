package org.infinity.rpc.core.exchange.cluster.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.ClusterHolder;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.annotation.Event;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@ThreadSafe
public class ConsumerListener<T> implements ClientListener {
    private       Protocol                             protocol;
    private       List<Url>                            registryUrls;
    private       Url                                  clientUrl;
    /**
     * The interface class of the consumer
     */
    private       Class<T>                             interfaceClass;
    private final Map<Url, List<ProviderRequester<T>>> requestersPerRegistryUrl = new ConcurrentHashMap<>();

    /**
     * Prohibit instantiate an instance outside the class
     */
    private ConsumerListener() {
    }

    public static <T> ConsumerListener<T> of(Class<T> interfaceClass, List<Url> registryUrls, Url clientUrl) {
        ConsumerListener<T> listener = new ConsumerListener<>();
        listener.interfaceClass = interfaceClass;
        listener.registryUrls = registryUrls;
        listener.clientUrl = clientUrl;
        listener.protocol = Protocol.getInstance(clientUrl.getProtocol());

        // Subscribe this client listener to all the registries
        listener.subscribe();
        return listener;
    }

    /**
     * Monitor the providers change event, e.g. child change event for zookeeper
     *
     * @param registryUrl  registry url
     * @param providerUrls provider urls
     */
    @Override
    @Event
    public synchronized void onSubscribe(Url registryUrl, List<Url> providerUrls) {
        if (CollectionUtils.isEmpty(providerUrls)) {
            log.info("No available providers found on registry: {} for now!", registryUrl.getUri());
            removeInactiveRegistry(registryUrl);
            return;
        }

        List<ProviderRequester<T>> newProviderRequesters = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            List<ProviderRequester<T>> providerRequesters = requestersPerRegistryUrl.get(registryUrl);
            ProviderRequester<T> providerRequester = findRequesterByProviderUrl(providerRequesters, providerUrl);
            if (providerRequester == null) {
                Url providerUrlCopy = providerUrl.copy();
                providerRequester = protocol.createRequester(interfaceClass, providerUrlCopy);
            }
            if (providerRequester != null) {
                newProviderRequesters.add(providerRequester);
            }
        }

        if (CollectionUtils.isEmpty(newProviderRequesters)) {
            removeInactiveRegistry(registryUrl);
            return;
        }

        requestersPerRegistryUrl.put(registryUrl, newProviderRequesters);
        refreshCluster();
    }

    private ProviderRequester<T> findRequesterByProviderUrl(List<ProviderRequester<T>> providerRequesters, Url providerUrl) {
        return CollectionUtils.isEmpty(providerRequesters) ? null :
                providerRequesters
                        .stream()
                        .filter(requester -> Objects.equals(providerUrl, requester.getProviderUrl()))
                        .findFirst()
                        .orElseGet(null);
    }

    /**
     * Remove the inactive registry
     *
     * @param inactiveRegistryUrl inactive registry url
     */
    private synchronized void removeInactiveRegistry(Url inactiveRegistryUrl) {
        if (requestersPerRegistryUrl.size() > 1) {
            requestersPerRegistryUrl.remove(inactiveRegistryUrl);
            refreshCluster();
        }
    }

    private synchronized void refreshCluster() {
        List<ProviderRequester<T>> allProviderRequesters = requestersPerRegistryUrl.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Loop all the cluster and update requesters
        List<ProviderCluster<T>> providerClusters = ClusterHolder.getInstance().getClusters();
        providerClusters.forEach(c -> c.onRefresh(allProviderRequesters));
    }

    /**
     * Subscribe this client listener to all the registries
     */
    private void subscribe() {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            // Bind this listener to the client
            registry.subscribe(clientUrl, this);
        }
    }
}

package org.infinity.rpc.core.exchange.cluster.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.cluster.ClusterHolder;
import org.infinity.rpc.core.exchange.request.ProtocolRequester;
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
    private final Map<Url, List<ProtocolRequester<T>>> requestersPerRegistryUrl = new ConcurrentHashMap<>();

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

        List<ProtocolRequester<T>> newProtocolRequesters = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            List<ProtocolRequester<T>> protocolRequesters = requestersPerRegistryUrl.get(registryUrl);
            ProtocolRequester<T> protocolRequester = findRequesterByProviderUrl(protocolRequesters, providerUrl);
            if (protocolRequester == null) {
                Url providerUrlCopy = providerUrl.copy();
                protocolRequester = protocol.createRequester(interfaceClass, providerUrlCopy);
            }
            if (protocolRequester != null) {
                newProtocolRequesters.add(protocolRequester);
            }
        }

        if (CollectionUtils.isEmpty(newProtocolRequesters)) {
            removeInactiveRegistry(registryUrl);
            return;
        }

        requestersPerRegistryUrl.put(registryUrl, newProtocolRequesters);
        refreshCluster();
    }

    private ProtocolRequester<T> findRequesterByProviderUrl(List<ProtocolRequester<T>> protocolRequesters, Url providerUrl) {
        return CollectionUtils.isEmpty(protocolRequesters) ? null :
                protocolRequesters
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
        List<ProtocolRequester<T>> allProtocolRequesters = requestersPerRegistryUrl.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Loop all the cluster and update requesters
        List<Cluster<T>> clusters = ClusterHolder.getInstance().getClusters();
        clusters.forEach(c -> c.onRefresh(allProtocolRequesters));
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

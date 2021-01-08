package org.infinity.rpc.core.exchange.cluster.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.ProviderClusterHolder;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.annotation.EventMarker;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@ThreadSafe
public class ConsumerListener<T> implements ClientListener {
    private       Protocol                          protocol;
    private       List<Url>                         registryUrls;
    private       Url                               clientUrl;
    /**
     * The interface class of the consumer
     */
    private       Class<T>                          interfaceClass;
    private final Map<Url, List<ProviderCaller<T>>> callersPerRegistryUrl = new ConcurrentHashMap<>();

    /**
     * Prevent instantiation of it outside the class
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
    @EventMarker
    public synchronized void onNotify(Url registryUrl, List<Url> providerUrls) {
        if (CollectionUtils.isEmpty(providerUrls)) {
            log.info("No available providers found on registry: {} for now!", registryUrl.getUri());
            removeInactiveRegistry(registryUrl);
            return;
        }

        List<ProviderCaller<T>> newProviderCallers = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            List<ProviderCaller<T>> providerCallers = callersPerRegistryUrl.get(registryUrl);
            ProviderCaller<T> providerCaller = findCallerByProviderUrl(providerCallers, providerUrl);
            if (providerCaller == null) {
                Url providerUrlCopy = providerUrl.copy();
                providerCaller = protocol.createProviderCaller(interfaceClass, providerUrlCopy);
            }
            if (providerCaller != null) {
                newProviderCallers.add(providerCaller);
            }
        }

        if (CollectionUtils.isEmpty(newProviderCallers)) {
            removeInactiveRegistry(registryUrl);
            return;
        }

        callersPerRegistryUrl.put(registryUrl, newProviderCallers);
        refreshCluster();
    }

    private ProviderCaller<T> findCallerByProviderUrl(List<ProviderCaller<T>> providerCallers, Url providerUrl) {
        return CollectionUtils.isEmpty(providerCallers) ? null :
                providerCallers
                        .stream()
                        .filter(caller -> Objects.equals(providerUrl, caller.getProviderUrl()))
                        .findFirst()
                        .orElseGet(null);
    }

    /**
     * Remove the inactive registry
     *
     * @param inactiveRegistryUrl inactive registry url
     */
    private synchronized void removeInactiveRegistry(Url inactiveRegistryUrl) {
        if (callersPerRegistryUrl.size() > 1) {
            callersPerRegistryUrl.remove(inactiveRegistryUrl);
            refreshCluster();
        }
    }

    private synchronized void refreshCluster() {
        List<ProviderCaller<T>> allProviderCallers = callersPerRegistryUrl.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Loop all the cluster and update callers
        @SuppressWarnings({"unchecked"})
        List<ProviderCluster<T>> providerClusters = ProviderClusterHolder.getInstance().getClusters();
        providerClusters.forEach(c -> c.refresh(allProviderCallers));
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

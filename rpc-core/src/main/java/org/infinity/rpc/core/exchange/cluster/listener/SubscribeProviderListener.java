package org.infinity.rpc.core.exchange.cluster.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.annotation.EventReceiver;
import org.infinity.rpc.utilities.annotation.EventSubscriber;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * todo: ClusterSupport
 * Listener used to subscribe providers change event,
 * method {@link SubscribeProviderListener#onNotify(Url, List)} will be invoked if providers change event occurs.
 *
 * @param <T>: The interface class of the consumer
 */
@Slf4j
@ThreadSafe
public class SubscribeProviderListener<T> implements ClientListener {
    /**
     * The interface class of the consumer
     */
    private       Class<T>                          interfaceClass;
    private       ProviderCluster<T>                providerCluster;
    private       Protocol                          protocol;
    private       List<Url>                         registryUrls;
    private       Url                               clientUrl;
    private final Map<Url, List<ProviderCaller<T>>> providerCallersPerRegistryUrl = new ConcurrentHashMap<>();

    /**
     * Prevent instantiation of it outside the class
     */
    private SubscribeProviderListener() {
    }

    public static <T> SubscribeProviderListener<T> of(Class<T> interfaceClass, ProviderCluster<T> providerCluster, List<Url> registryUrls, Url clientUrl) {
        SubscribeProviderListener<T> listener = new SubscribeProviderListener<>();
        listener.interfaceClass = interfaceClass;
        listener.providerCluster = providerCluster;
        listener.registryUrls = registryUrls;
        listener.clientUrl = clientUrl;
        listener.protocol = Protocol.getInstance(clientUrl.getProtocol());

        // IMPORTANT: Subscribe this client listener to all the registries
        // So when providers change event occurs, it can invoke onNotify() method.
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
    @EventReceiver("providersDiscoveryEvent")
    public synchronized void onNotify(Url registryUrl, List<Url> providerUrls) {
        if (CollectionUtils.isEmpty(providerUrls)) {
            log.warn("No active providers found on registry [{}]", registryUrl.getUri());
            removeInactiveRegistry(registryUrl);
            return;
        }

        List<ProviderCaller<T>> newProviderCallers = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            // Find provider caller associated with the provider url
            ProviderCaller<T> providerCaller = findCallerByProviderUrl(registryUrl, providerUrl);
            if (providerCaller == null) {
                providerCaller = protocol.createProviderCaller(interfaceClass, providerUrl.copy());
            }
            newProviderCallers.add(providerCaller);
        }

        if (CollectionUtils.isEmpty(newProviderCallers)) {
            log.warn("No active provider caller!");
        }
        providerCallersPerRegistryUrl.put(registryUrl, newProviderCallers);
        refreshCluster();
    }

    private ProviderCaller<T> findCallerByProviderUrl(Url registryUrl, Url providerUrl) {
        List<ProviderCaller<T>> providerCallers = providerCallersPerRegistryUrl.get(registryUrl);
        return CollectionUtils.isEmpty(providerCallers) ? null :
                providerCallers
                        .stream()
                        .filter(caller -> Objects.equals(providerUrl, caller.getProviderUrl()))
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Remove the inactive registry
     *
     * @param inactiveRegistryUrl inactive registry url
     */
    private synchronized void removeInactiveRegistry(Url inactiveRegistryUrl) {
        if (providerCallersPerRegistryUrl.size() > 1) {
            providerCallersPerRegistryUrl.remove(inactiveRegistryUrl);
            refreshCluster();
        }
    }

    private synchronized void refreshCluster() {
        List<ProviderCaller<T>> providerCallers = providerCallersPerRegistryUrl.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Refresh provider callers to AbstractLoadBalancer
        providerCluster.refresh(providerCallers);
    }

    /**
     * Subscribe this client listener to all the registries
     */
    @EventSubscriber("providersDiscoveryEvent")
    private void subscribe() {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            // Bind this listener to the client
            registry.subscribe(clientUrl, this);
        }
    }

    @Override
    public String toString() {
        return SubscribeProviderListener.class.getSimpleName().concat(":").concat(interfaceClass.getName());
    }
}

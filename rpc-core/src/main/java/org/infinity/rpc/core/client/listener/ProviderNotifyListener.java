package org.infinity.rpc.core.client.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.cluster.ProviderCluster;
import org.infinity.rpc.core.client.request.ProviderCaller;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.annotation.EventReceiver;
import org.infinity.rpc.utilities.concurrent.ThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * todo: see ClusterSupport
 * Listener used to subscribe providers change event,
 * method {@link ProviderNotifyListener#onNotify(Url, List)} will be invoked if providers change event occurs.
 *
 * @param <T>: The interface class of the consumer
 */
@Slf4j
@ThreadSafe
public class ProviderNotifyListener<T> implements ClientListener {
    protected     ProviderCluster<T>                providerCluster;
    /**
     * The interface class of the consumer
     */
    protected     Class<T>                          interfaceClass;
    protected     Protocol                          protocol;
    private final Map<Url, List<ProviderCaller<T>>> providerCallersPerRegistryUrl = new ConcurrentHashMap<>();

    protected ProviderNotifyListener() {
    }

    /**
     * Pass provider cluster to listener, listener will update provider cluster after provider urls changed
     *
     * @param providerCluster provider cluster
     * @param interfaceClass  The interface class of the consumer
     * @param protocol        protocol
     * @param <T>             The interface class of the consumer
     * @return listener
     */
    public static <T> ProviderNotifyListener<T> of(ProviderCluster<T> providerCluster, Class<T> interfaceClass, String protocol) {
        ProviderNotifyListener<T> listener = new ProviderNotifyListener<>();
        listener.providerCluster = providerCluster;
        listener.interfaceClass = interfaceClass;
        listener.protocol = Protocol.getInstance(protocol);
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

    @Override
    public String toString() {
        return ProviderNotifyListener.class.getSimpleName().concat(":").concat(interfaceClass.getName());
    }
}

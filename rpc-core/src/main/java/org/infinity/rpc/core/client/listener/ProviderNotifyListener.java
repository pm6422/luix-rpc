package org.infinity.rpc.core.client.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.cluster.InvokerCluster;
import org.infinity.rpc.core.client.request.Invokable;
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
 */
@Slf4j
@ThreadSafe
public class ProviderNotifyListener implements ClientListener {
    protected     InvokerCluster            invokerCluster;
    /**
     * The interface class name of the consumer
     */
    protected     String                    interfaceName;
    protected     Protocol                  protocol;
    private final Map<Url, List<Invokable>> providerCallersPerRegistryUrl = new ConcurrentHashMap<>();

    protected ProviderNotifyListener() {
    }

    /**
     * Pass provider invoker cluster to listener, listener will update provider invoker cluster after provider urls changed
     *
     * @param invokerCluster provider invoker cluster
     * @param interfaceName  The interface class name of the consumer
     * @param protocol       protocol
     * @return listener listener
     */
    public static ProviderNotifyListener of(InvokerCluster invokerCluster, String interfaceName, String protocol) {
        ProviderNotifyListener listener = new ProviderNotifyListener();
        listener.invokerCluster = invokerCluster;
        listener.interfaceName = interfaceName;
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

        List<Invokable> newImporters = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            // Find provider caller associated with the provider url
            Invokable importer = findCallerByProviderUrl(registryUrl, providerUrl);
            if (importer == null) {
                importer = protocol.refer(interfaceName, providerUrl.copy());
            }
            newImporters.add(importer);
        }

        if (CollectionUtils.isEmpty(newImporters)) {
            log.warn("No active provider caller!");
        }
        providerCallersPerRegistryUrl.put(registryUrl, newImporters);
        refreshCluster();
    }

    private Invokable findCallerByProviderUrl(Url registryUrl, Url providerUrl) {
        List<Invokable> importers = providerCallersPerRegistryUrl.get(registryUrl);
        return CollectionUtils.isEmpty(importers) ? null :
                importers
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
        List<Invokable> importers = providerCallersPerRegistryUrl.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Refresh provider callers to AbstractLoadBalancer
        invokerCluster.refresh(importers);
    }

    @Override
    public String toString() {
        return ProviderNotifyListener.class.getSimpleName().concat(":").concat(interfaceName);
    }
}

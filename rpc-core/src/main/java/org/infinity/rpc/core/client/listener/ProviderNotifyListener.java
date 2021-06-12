package org.infinity.rpc.core.client.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.invoker.ServiceInvoker;
import org.infinity.rpc.core.client.sender.Sendable;
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
    protected     ServiceInvoker           serviceInvoker;
    /**
     * The interface class name of the consumer
     */
    protected     String                   interfaceName;
    protected     Protocol                 protocol;
    protected     ProviderProcessable      providerProcessor;
    private final Map<Url, List<Sendable>> invokersPerRegistryUrl = new ConcurrentHashMap<>();

    protected ProviderNotifyListener() {
    }

    /**
     * Pass service provider invoker to listener, listener will update service invoker after provider urls changed
     *
     * @param serviceInvoker    service invoker
     * @param interfaceName     interface class name of the consumer
     * @param protocol          protocol
     * @param providerProcessor provider processor
     * @return listener listener
     */
    public static ProviderNotifyListener of(ServiceInvoker serviceInvoker, String interfaceName, String protocol,
                                            ProviderProcessable providerProcessor) {
        ProviderNotifyListener listener = new ProviderNotifyListener();
        listener.serviceInvoker = serviceInvoker;
        listener.interfaceName = interfaceName;
        listener.protocol = Protocol.getInstance(protocol);
        listener.providerProcessor = providerProcessor;
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
        if (providerProcessor != null) {
            providerProcessor.process(registryUrl, providerUrls, interfaceName);
        }
        if (CollectionUtils.isEmpty(providerUrls)) {
            log.warn("No active providers found on registry [{}]", registryUrl.getUri());
            removeInactiveRegistry(registryUrl);
            return;
        }

        List<Sendable> newSenders = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            // Find provider invoker associated with the provider url
            Sendable invoker = findInvokerByProviderUrl(registryUrl, providerUrl);
            if (invoker == null) {
                invoker = protocol.refer(interfaceName, providerUrl.copy());
            }
            newSenders.add(invoker);
        }

        if (CollectionUtils.isEmpty(newSenders)) {
            log.warn("No active provider sender!");
        }
        invokersPerRegistryUrl.put(registryUrl, newSenders);
        refreshCluster();
    }

    private Sendable findInvokerByProviderUrl(Url registryUrl, Url providerUrl) {
        List<Sendable> invokers = invokersPerRegistryUrl.get(registryUrl);
        return CollectionUtils.isEmpty(invokers) ? null :
                invokers.stream()
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
        if (invokersPerRegistryUrl.size() > 1) {
            invokersPerRegistryUrl.remove(inactiveRegistryUrl);
            refreshCluster();
        }
    }

    private synchronized void refreshCluster() {
        List<Sendable> invokers = invokersPerRegistryUrl.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Refresh provider invokers to AbstractLoadBalancer
        serviceInvoker.getFaultTolerance().getLoadBalancer().refresh(invokers);
    }

    @Override
    public String toString() {
        return ProviderNotifyListener.class.getSimpleName().concat(":").concat(interfaceName);
    }
}

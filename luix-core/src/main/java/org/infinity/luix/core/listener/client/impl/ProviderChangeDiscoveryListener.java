package org.infinity.luix.core.listener.client.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.core.client.invoker.ServiceInvoker;
import org.infinity.luix.core.client.sender.Sendable;
import org.infinity.luix.core.listener.client.ConsumerListener;
import org.infinity.luix.core.listener.client.ProviderProcessable;
import org.infinity.luix.core.protocol.Protocol;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.annotation.EventReceiver;
import org.infinity.luix.utilities.concurrent.ThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.infinity.luix.core.constant.ProtocolConstants.PROTOCOL_VAL_DEFAULT;

/**
 * todo: see ClusterSupport
 * Listener used to subscribe providers change event,
 * method {@link ProviderChangeDiscoveryListener#onNotify(Url, Url, List)} will be invoked if providers change event occurs.
 */
@Slf4j
@ThreadSafe
public class ProviderChangeDiscoveryListener implements ConsumerListener {
    protected     ServiceInvoker           serviceInvoker;
    protected     Protocol                 protocol;
    protected     String                   interfaceName;
    protected     String                   form;
    protected     ProviderProcessable      providerProcessor;
    private final Map<Url, List<Sendable>> sendersPerRegistryUrl = new ConcurrentHashMap<>();

    /**
     * Pass service provider invoker to listener, listener will update service invoker after provider urls changed
     *
     * @param serviceInvoker    service invoker
     * @param protocolName      protocol name
     * @param interfaceName     interface class name of the consumer
     * @param form              form
     * @param providerProcessor provider processor
     * @return listener listener
     */
    public static ProviderChangeDiscoveryListener of(ServiceInvoker serviceInvoker,
                                                     String protocolName,
                                                     String interfaceName,
                                                     String form,
                                                     ProviderProcessable providerProcessor) {
        ProviderChangeDiscoveryListener listener = new ProviderChangeDiscoveryListener();
        listener.serviceInvoker = serviceInvoker;
        listener.protocol = Protocol.getInstance(defaultIfEmpty(protocolName, PROTOCOL_VAL_DEFAULT));
        listener.interfaceName = interfaceName;
        listener.form = form;
        listener.providerProcessor = providerProcessor;
        return listener;
    }

    /**
     * Monitor the providers change event, e.g. child change event for zookeeper
     *
     * @param registryUrl  registry url
     * @param consumerUrl  consumer url
     * @param providerUrls provider urls
     */
    @Override
    @EventReceiver("providersDiscoveryEvent")
    public synchronized void onNotify(Url registryUrl, Url consumerUrl, List<Url> providerUrls) {
        if (providerProcessor != null) {
            providerProcessor.process(registryUrl, interfaceName, providerUrls);
        }
        if (CollectionUtils.isEmpty(providerUrls)) {
            log.warn("No active providers found on registry [{}]", registryUrl.getUri());
            removeInactiveRegistry(registryUrl);
            return;
        }

        List<Sendable> newSenders = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            if (!providerUrl.getForm().equals(defaultString(form))) {
                continue;
            }
            // Find provider invoker associated with the provider url
            Sendable invoker = findInvokerByProviderUrl(registryUrl, providerUrl);
            if (invoker == null) {
                invoker = protocol.createRequestSender(interfaceName, providerUrl.copy());
            }
            newSenders.add(invoker);
        }

        if (CollectionUtils.isEmpty(newSenders)) {
            log.warn("No active provider sender!");
        }
        sendersPerRegistryUrl.put(registryUrl, newSenders);
        refreshSenders();
    }

    private Sendable findInvokerByProviderUrl(Url registryUrl, Url providerUrl) {
        List<Sendable> senders = sendersPerRegistryUrl.get(registryUrl);
        return CollectionUtils.isEmpty(senders) ? null :
                senders.stream()
                        .filter(caller -> Objects.equals(providerUrl, caller.getProviderUrl()))
                        .findFirst()
                        .orElse(null);
    }

    /**
     * Remove the inactive registry
     *
     * @param registryUrl inactive registry url
     */
    private synchronized void removeInactiveRegistry(Url registryUrl) {
        sendersPerRegistryUrl.remove(registryUrl);
        refreshSenders();
    }

    private synchronized void refreshSenders() {
        List<Sendable> senders = sendersPerRegistryUrl.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // Refresh provider senders to AbstractLoadBalancer
        serviceInvoker.getFaultTolerance().getLoadBalancer().refresh(senders);
    }

    @Override
    public String toString() {
        return ProviderChangeDiscoveryListener.class.getSimpleName()
                .concat(":").concat(interfaceName)
                .concat(":").concat(defaultString(form));
    }
}

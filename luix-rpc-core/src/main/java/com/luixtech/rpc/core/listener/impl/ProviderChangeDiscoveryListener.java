package com.luixtech.rpc.core.listener.impl;

import com.luixtech.rpc.core.client.invoker.ServiceInvoker;
import com.luixtech.rpc.core.client.sender.Sendable;
import com.luixtech.rpc.core.constant.ProtocolConstants;
import com.luixtech.rpc.core.listener.ProviderDiscoveryListener;
import com.luixtech.rpc.core.protocol.Protocol;
import com.luixtech.rpc.core.url.Url;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.luixtech.utilities.annotation.EventReceiver;
import com.luixtech.utilities.concurrent.ThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;

/**
 * todo: see ClusterSupport
 * Listener used to subscribe providers change event,
 * method {@link ProviderChangeDiscoveryListener#onNotify(Url, String, List)} will be invoked if providers change event occurs.
 */
@Slf4j
@ThreadSafe
public class ProviderChangeDiscoveryListener implements ProviderDiscoveryListener {
    private       Url                      consumerUrl;
    private       ServiceInvoker           serviceInvoker;
    private       Protocol                 protocol;
    private final Map<Url, List<Sendable>> sendersPerRegistryUrl = new ConcurrentHashMap<>();

    /**
     * Pass service provider invoker to listener, listener will update service invoker after provider urls changed
     *
     * @param serviceInvoker service invoker
     * @param consumerUrl    consumer url
     * @return listener listener
     */
    public static ProviderChangeDiscoveryListener of(Url consumerUrl, ServiceInvoker serviceInvoker) {
        ProviderChangeDiscoveryListener listener = new ProviderChangeDiscoveryListener();
        listener.consumerUrl = consumerUrl;
        listener.serviceInvoker = serviceInvoker;
        listener.protocol = Protocol.getInstance(defaultIfEmpty(consumerUrl.getProtocol(), ProtocolConstants.PROTOCOL_VAL_DEFAULT));
        return listener;
    }

    /**
     * Monitor the providers change event, e.g. child change event for zookeeper
     *
     * @param registryUrl   registry url
     * @param interfaceName interface name
     * @param providerUrls  provider urls
     */
    @Override
    @EventReceiver("providersDiscoveryEvent")
    public synchronized void onNotify(Url registryUrl, String interfaceName, List<Url> providerUrls) {
        if (CollectionUtils.isEmpty(providerUrls)) {
            log.warn("No active providers found on registry [{}]", registryUrl.getUri());
            removeInactiveRegistry(registryUrl);
            return;
        }
        log.info("Discovered active providers {} on registry [{}]", providerUrls, registryUrl.getUri());
        List<Sendable> newSenders = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            if (!providerUrl.getForm().equals(consumerUrl.getForm())) {
                continue;
            }
            // Find provider invoker associated with the provider url
            Sendable sender = findInvokerByProviderUrl(registryUrl, providerUrl);
            if (sender == null) {
                sender = protocol.createRequestSender(consumerUrl.getPath(), providerUrl.copy());
            }
            newSenders.add(sender);
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
                .concat(":").concat(consumerUrl.getPath())
                .concat(":").concat(defaultString(consumerUrl.getForm()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass().getName(), this.consumerUrl.getIdentity());
    }

    @Override
    public boolean equals(Object o) {
        return Objects.equals(this.getClass().getName(), o.getClass().getName())
                && Objects.equals(this.consumerUrl.getIdentity(), ((ProviderChangeDiscoveryListener) o).consumerUrl.getIdentity());
    }
}

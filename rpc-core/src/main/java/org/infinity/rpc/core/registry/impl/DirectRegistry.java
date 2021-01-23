package org.infinity.rpc.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.registry.AbstractRegistry;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.destory.Cleanable;
import org.infinity.rpc.utilities.network.AddressUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.constant.ServiceConstants.REGISTRY_VALUE_DIRECT;
import static org.infinity.rpc.core.url.Url.PARAM_ADDRESS;

@Slf4j
@ThreadSafe
public class DirectRegistry extends AbstractRegistry implements Cleanable {
    private final List<Url>        directUrls    = new ArrayList<>();
    private final Map<Url, Object> subscribeUrls = new ConcurrentHashMap<>();

    public DirectRegistry(Url registryUrl) {
        super(registryUrl);
        parseDirectUrls(registryUrl.getParameter(PARAM_ADDRESS));
    }

    private void parseDirectUrls(String address) {
        List<Pair<String, Integer>> hostPortList = AddressUtils.parseAddress(address);
        hostPortList.forEach(hostPortPair -> {
            // Use empty string as path
            directUrls.add(Url.of(REGISTRY_VALUE_DIRECT, hostPortPair.getLeft(), hostPortPair.getRight(), StringUtils.EMPTY));
        });
    }

    @Override
    protected void doRegister(Url url) {
        // Do nothing
    }

    @Override
    protected void doUnregister(Url url) {
        // Do nothing
    }

    @Override
    protected void doActivate(Url url) {
        // Do nothing
    }

    @Override
    protected void doDeactivate(Url url) {
        // Do nothing
    }

    @Override
    protected List<Url> discoverActiveProviders(Url clientUrl) {
        return null;
    }

    @Override
    protected synchronized void doSubscribe(Url clientUrl, ClientListener listener) {
        subscribeUrls.putIfAbsent(clientUrl, 1);
        // Notify
        listener.onNotify(registryUrl, doDiscover(clientUrl));
    }

    @Override
    protected synchronized void doUnsubscribe(Url clientUrl, ClientListener listener) {
        subscribeUrls.remove(clientUrl);
        // Notify
        listener.onNotify(registryUrl, doDiscover(clientUrl));
    }

    @Override
    protected void subscribeServiceListener(Url clientUrl, ServiceListener listener) {
        // Do nothing
    }

    @Override
    protected void unsubscribeServiceListener(Url clientUrl, ServiceListener listener) {
        // Do nothing
    }

    @Override
    protected List<Url> doDiscover(Url clientUrl) {
        return directUrls
                .stream()
                .map(directUrl -> Url.of(clientUrl.getProtocol(), directUrl.getHost(), directUrl.getPort(), StringUtils.EMPTY))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> discoverActiveProviderAddress(String providerPath) {
        // TODO
        return null;
    }

    @Override
    public void registerApplication(ApplicationExtConfig app) {
        // Do nothing
    }

    @Override
    public void registerApplicationProvider(String appName, Url providerUrl) {
        // Do nothing
    }

    @Override
    public void cleanup() {
        // Do nothing
    }
}

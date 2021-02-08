package org.infinity.rpc.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.registry.AbstractRegistry;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.destory.Cleanable;
import org.infinity.rpc.utilities.network.AddressUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.constant.ServiceConstants.REGISTRY_VALUE_DIRECT;

@Slf4j
@ThreadSafe
public class DirectRegistry extends AbstractRegistry implements Cleanable {
    private final List<Url> directUrls;

    public DirectRegistry(Url registryUrl) {
        super(registryUrl);
        directUrls = parseDirectUrls(registryUrl.getAddress());
    }

    private List<Url> parseDirectUrls(String address) {
        List<Url> urls = new ArrayList<>();
        List<Pair<String, Integer>> hostPortList = AddressUtils.parseAddress(address);
        hostPortList.forEach(hostPortPair -> {
            // Use empty string as path
            urls.add(Url.of(REGISTRY_VALUE_DIRECT, hostPortPair.getLeft(), hostPortPair.getRight(), StringUtils.EMPTY));
        });
        return urls;
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
        List<Url> result = new ArrayList<>(directUrls.size());
        for (Url directUrl : directUrls) {
            Url tmp = clientUrl.copy();
            tmp.setHost(directUrl.getHost());
            tmp.setPort(directUrl.getPort());
            result.add(tmp);
        }
        return result;
    }

    @Override
    protected synchronized void doSubscribe(Url clientUrl, ClientListener listener) {
        // Notify
        listener.onNotify(registryUrl, doDiscover(clientUrl));
    }

    @Override
    protected synchronized void doUnsubscribe(Url clientUrl, ClientListener listener) {
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
        throw new UnsupportedOperationException();
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
    public List<String> getAllProviderGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ApplicationExtConfig> getAllApps() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Map<String, List<AddressInfo>>> getAllProviders(String group) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cleanup() {
        // Do nothing
    }
}

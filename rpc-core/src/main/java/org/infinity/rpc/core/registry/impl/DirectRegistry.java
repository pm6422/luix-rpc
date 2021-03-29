package org.infinity.rpc.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.registry.AbstractRegistry;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.concurrent.ThreadSafe;
import org.infinity.rpc.utilities.destory.Cleanable;
import org.infinity.rpc.utilities.network.AddressUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.core.constant.ConsumerConstants.DIRECT_ADDRESSES;

@Slf4j
@ThreadSafe
public class DirectRegistry extends AbstractRegistry implements Cleanable {
    private final List<Pair<String, Integer>> providerHostAndPortList;

    public DirectRegistry(Url registryUrl) {
        super(registryUrl);
        providerHostAndPortList = AddressUtils.parseAddress(registryUrl.getOption(DIRECT_ADDRESSES));
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
    protected synchronized void doSubscribe(Url clientUrl, ClientListener listener) {
        List<Url> providerUrls = doDiscover(clientUrl);
        // Notify
        listener.onNotify(registryUrl, providerUrls);
    }

    @Override
    protected synchronized void doUnsubscribe(Url clientUrl, ClientListener listener) {
        List<Url> providerUrls = doDiscover(clientUrl);
        // Notify
        listener.onNotify(registryUrl, providerUrls);
    }

    @Override
    protected void subscribeServiceListener(Url clientUrl, ServiceListener listener) {
        // Do nothing
    }

    @Override
    protected void unsubscribeServiceListener(Url clientUrl, ServiceListener listener) {
        // Do nothing
    }

    /**
     * Discover the provider urls
     *
     * @param clientUrl client url
     * @return provider urls
     */
    @Override
    protected List<Url> doDiscover(Url clientUrl) {
        List<Url> providerUrls = new ArrayList<>(providerHostAndPortList.size());
        for (Pair<String, Integer> directProviderUrl : providerHostAndPortList) {
            Url clientUrlCopy = clientUrl.copy();
            // Convert client url to provider url
            clientUrlCopy.setHost(directProviderUrl.getLeft());
            clientUrlCopy.setPort(directProviderUrl.getRight());
            clientUrlCopy.addOption(Url.PARAM_TYPE, Url.PARAM_TYPE_PROVIDER);
            providerUrls.add(clientUrlCopy);
        }
        return providerUrls;
    }

    @Override
    protected List<Url> discoverActiveProviders(Url clientUrl) {
        return doDiscover(clientUrl);
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
    public List<String> getAllProviderForms() {
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

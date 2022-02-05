package org.infinity.luix.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.luix.core.listener.GlobalConsumerDiscoveryListener;
import org.infinity.luix.core.listener.ProviderDiscoveryListener;
import org.infinity.luix.core.registry.AbstractRegistry;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.concurrent.ThreadSafe;
import org.infinity.luix.utilities.destory.Destroyable;
import org.infinity.luix.utilities.network.AddressUtils;

import java.util.ArrayList;
import java.util.List;

import static org.infinity.luix.core.constant.ConsumerConstants.PROVIDER_ADDRESSES;

@Slf4j
@ThreadSafe
public class NoRegistry extends AbstractRegistry implements Destroyable {
    private final List<Pair<String, Integer>> providerHostAndPortList;

    public NoRegistry(Url registryUrl) {
        super(registryUrl);
        providerHostAndPortList = AddressUtils.parseAddress(registryUrl.getOption(PROVIDER_ADDRESSES));
    }

    @Override
    protected void doRegister(Url url) {
        // Do nothing
    }

    @Override
    protected void doDeregister(Url url) {
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
    protected synchronized void doSubscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        List<Url> providerUrls = doDiscover(consumerUrl);
        // Notify
        listener.onNotify(registryUrl, consumerUrl.getPath(), providerUrls);
    }

    @Override
    protected synchronized void doUnsubscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        List<Url> providerUrls = doDiscover(consumerUrl);
        // Notify
        listener.onNotify(registryUrl, consumerUrl.getPath(), providerUrls);
    }

    @Override
    protected void subscribeListener(Url consumerUrl, ProviderDiscoveryListener listener) {
        // Do nothing
    }

    @Override
    protected void unsubscribeListener(Url consumerUrl, ProviderDiscoveryListener listener) {
        // Do nothing
    }

    /**
     * Discover the provider urls
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    @Override
    protected List<Url> doDiscover(Url consumerUrl) {
        List<Url> providerUrls = new ArrayList<>(providerHostAndPortList.size());
        for (Pair<String, Integer> directProviderUrl : providerHostAndPortList) {
            Url consumerUrlCopy = consumerUrl.copy();
            // Convert consumer url to provider url
            consumerUrlCopy.setHost(directProviderUrl.getLeft());
            consumerUrlCopy.setPort(directProviderUrl.getRight());
            consumerUrlCopy.addOption(Url.PARAM_TYPE, Url.PARAM_TYPE_PROVIDER);
            providerUrls.add(consumerUrlCopy);
        }
        return providerUrls;
    }

    @Override
    public List<Url> discoverProviders(Url consumerUrl) {
        return doDiscover(consumerUrl);
    }

    @Override
    public List<Url> discover() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void subscribe(GlobalConsumerDiscoveryListener listener) {
        // Do nothing
    }

    @Override
    public void destroy() {
        // Do nothing
    }
}

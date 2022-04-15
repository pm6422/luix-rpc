package com.luixtech.rpc.core.registry.impl;

import com.luixtech.rpc.core.constant.ConsumerConstants;
import com.luixtech.rpc.core.listener.GlobalConsumerDiscoveryListener;
import com.luixtech.rpc.core.listener.ProviderDiscoveryListener;
import com.luixtech.rpc.core.registry.AbstractRegistry;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.destory.Destroyable;
import com.luixtech.utilities.network.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ThreadSafe
public class NoRegistry extends AbstractRegistry implements Destroyable {
    private final List<Pair<String, Integer>> providerHostAndPortList;

    public NoRegistry(Url registryUrl) {
        super(registryUrl);
        providerHostAndPortList = AddressUtils.parseAddress(registryUrl.getOption(ConsumerConstants.PROVIDER_ADDRESSES));
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
    public synchronized void subscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        List<Url> providerUrls = doDiscoverActive(consumerUrl);
        // Notify
        listener.onNotify(registryUrl, consumerUrl.getPath(), providerUrls);
    }

    @Override
    public synchronized void unsubscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        List<Url> providerUrls = doDiscoverActive(consumerUrl);
        // Notify
        listener.onNotify(registryUrl, consumerUrl.getPath(), providerUrls);
    }

    @Override
    public void subscribe(GlobalConsumerDiscoveryListener listener) {
        // Do nothing
    }

    @Override
    public void unsubscribe(GlobalConsumerDiscoveryListener listener) {
        // Do nothing
    }

    /**
     * Discover the provider urls
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    @Override
    protected List<Url> doDiscoverActive(Url consumerUrl) {
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
    public List<Url> discoverProviders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() {
        // Do nothing
    }
}

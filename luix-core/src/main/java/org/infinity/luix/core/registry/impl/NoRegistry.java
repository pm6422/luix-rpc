package org.infinity.luix.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.luix.core.registry.AbstractRegistry;
import org.infinity.luix.core.registry.listener.ClientListener;
import org.infinity.luix.core.registry.listener.ProviderListener;
import org.infinity.luix.core.server.listener.ConsumerProcessable;
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
    protected synchronized void doSubscribe(Url consumerUrl, ClientListener listener) {
        List<Url> providerUrls = doDiscover(consumerUrl);
        // Notify
        listener.onNotify(registryUrl, providerUrls);
    }

    @Override
    protected synchronized void doUnsubscribe(Url consumerUrl, ClientListener listener) {
        List<Url> providerUrls = doDiscover(consumerUrl);
        // Notify
        listener.onNotify(registryUrl, providerUrls);
    }

    @Override
    protected void subscribeProviderListener(Url consumerUrl, ProviderListener listener) {
        // Do nothing
    }

    @Override
    protected void unsubscribeProviderListener(Url consumerUrl, ProviderListener listener) {
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
    protected List<Url> discoverActiveProviders(Url consumerUrl) {
        return doDiscover(consumerUrl);
    }

    @Override
    public List<Url> getAllProviderUrls() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void subscribeAllConsumerChanges(ConsumerProcessable consumerProcessor) {

    }

    @Override
    public void destroy() {
        // Do nothing
    }
}

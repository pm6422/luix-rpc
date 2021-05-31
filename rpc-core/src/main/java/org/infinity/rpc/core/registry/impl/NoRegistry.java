package org.infinity.rpc.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.registry.AbstractRegistry;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.concurrent.ThreadSafe;
import org.infinity.rpc.utilities.destory.Cleanable;
import org.infinity.rpc.utilities.network.IpUtils;

import java.util.ArrayList;
import java.util.List;

import static org.infinity.rpc.core.constant.ConsumerConstants.PROVIDER_ADDRESSES;

@Slf4j
@ThreadSafe
public class NoRegistry extends AbstractRegistry implements Cleanable {
    private final List<Pair<String, Integer>> providerHostAndPortList;

    public NoRegistry(Url registryUrl) {
        super(registryUrl);
        providerHostAndPortList = IpUtils.parseAddress(registryUrl.getOption(PROVIDER_ADDRESSES));
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
    protected void subscribeServiceListener(Url consumerUrl, ServiceListener listener) {
        // Do nothing
    }

    @Override
    protected void unsubscribeServiceListener(Url consumerUrl, ServiceListener listener) {
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
    public List<String> discoverActiveProviderAddress(String providerPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAllProviderPaths() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cleanup() {
        // Do nothing
    }
}

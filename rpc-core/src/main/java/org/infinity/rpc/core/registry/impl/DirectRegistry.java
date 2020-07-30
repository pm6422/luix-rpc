package org.infinity.rpc.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.registry.AbstractRegistry;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.destory.Cleanable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

@Slf4j
@ThreadSafe
public class DirectRegistry extends AbstractRegistry implements Cleanable {

    public DirectRegistry(Url registryUrl) {
        super(registryUrl);
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
    protected void doSubscribe(Url url, ClientListener listener) {

    }

    @Override
    protected void doUnsubscribe(Url url, ClientListener listener) {

    }

    @Override
    protected void subscribeServiceListener(Url clientUrl, ServiceListener listener) {

    }

    @Override
    protected void unsubscribeServiceListener(Url clientUrl, ServiceListener listener) {

    }

    @Override
    protected List<Url> doDiscover(Url url) {
        return null;
    }

    @Override
    public List<String> discoverActiveProviderAddress(String providerPath) {
        return null;
    }

    @Override
    public void registerApplication(App app) {

    }

    @Override
    public void registerApplicationProvider(App app, Url providerUrl) {

    }

    @Override
    public void cleanup() {

    }
}

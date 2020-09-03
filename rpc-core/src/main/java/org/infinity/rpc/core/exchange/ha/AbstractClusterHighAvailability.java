package org.infinity.rpc.core.exchange.ha;

import org.infinity.rpc.core.url.Url;

/**
 *
 * @param <T>: The interface class of the provider
 */
public abstract class AbstractClusterHighAvailability<T> implements ClusterHighAvailability<T> {

    protected Url clientUrl;

    // TODO: check use
    @Override
    public void setClientUrl(Url clientUrl) {
        this.clientUrl = clientUrl;
    }

    // TODO: check use
    @Override
    public Url getClientUrl() {
        return clientUrl;
    }
}
package org.infinity.rpc.core.client.faulttolerance;

import org.infinity.rpc.core.url.Url;

/**
 * @param <T>: The interface class of the provider
 */
public abstract class AbstractFaultTolerance<T> implements FaultTolerance<T> {

    protected Url clientUrl;

    @Override
    public void setClientUrl(Url clientUrl) {
        this.clientUrl = clientUrl;
    }

    @Override
    public Url getClientUrl() {
        return clientUrl;
    }
}
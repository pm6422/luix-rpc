package org.infinity.rpc.core.exchange.ha;

import org.infinity.rpc.core.url.Url;

public abstract class AbstractHighAvailability<T> implements HighAvailability<T> {

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
package org.infinity.rpc.core.exchange.ha;

import org.infinity.rpc.core.registry.Url;

public abstract class AbstractHighAvailability<T> implements HighAvailability<T> {

    protected Url clientUrl;

    @Override
    public void setClientUrl(Url clientUrl) {
        this.clientUrl = clientUrl;
    }
}
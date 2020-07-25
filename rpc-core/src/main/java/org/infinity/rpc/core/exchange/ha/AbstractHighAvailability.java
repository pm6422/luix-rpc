package org.infinity.rpc.core.exchange.ha;

import org.infinity.rpc.core.registry.Url;

public abstract class AbstractHighAvailability<T> implements HighAvailability<T> {

    protected Url url;

    @Override
    public void setUrl(Url url) {
        this.url = url;
    }
}
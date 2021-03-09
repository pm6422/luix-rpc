package org.infinity.rpc.core.client.faulttolerance;

import org.infinity.rpc.core.url.Url;

/**
 *
 */
public abstract class AbstractFaultTolerance implements FaultTolerance {

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
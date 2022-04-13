package com.luixtech.luixrpc.core.server.exposer;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.luixrpc.core.url.Url;

@Slf4j
public abstract class AbstractProviderExposer implements ProviderExposable {
    protected volatile boolean active = false;
    protected          Url     providerUrl;

    public AbstractProviderExposer(Url providerUrl) {
        this.providerUrl = providerUrl;
    }

    @Override
    public Url getProviderUrl() {
        return providerUrl;
    }

    @Override
    public synchronized void expose() {
        boolean result = doExpose();
        if (result) {
            active = true;
            log.info("Exposed provider [{}]", providerUrl);
        } else {
            throw new RpcFrameworkException("Failed to initialize provider exposer");
        }
    }

    /**
     * Do expose
     *
     * @return {@code true} if it was initialized and {@code false} otherwise
     */
    protected abstract boolean doExpose();

    @Override
    public String toString() {
        return this.getClass().getSimpleName().concat(":").concat(providerUrl.toFullStr());
    }
}

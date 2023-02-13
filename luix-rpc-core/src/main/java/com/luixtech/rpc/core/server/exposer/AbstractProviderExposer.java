package com.luixtech.rpc.core.server.exposer;

import com.luixtech.rpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.rpc.core.url.Url;
import lombok.extern.slf4j.Slf4j;

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

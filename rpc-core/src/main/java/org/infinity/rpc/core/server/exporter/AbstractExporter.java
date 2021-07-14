package org.infinity.rpc.core.server.exporter;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.url.Url;

@Slf4j
public abstract class AbstractExporter implements Exportable {
    protected          Url     providerUrl;
    protected volatile boolean initialized = false;
    protected volatile boolean active      = false;

    public AbstractExporter(Url providerUrl) {
        this.providerUrl = providerUrl;
    }

    @Override
    public Url getProviderUrl() {
        return providerUrl;
    }

    @Override
    public synchronized void init() {
        if (initialized) {
            log.warn(this.getClass().getSimpleName() + " node already init: " + this);
            return;
        }

        boolean result = doInit();

        if (!result) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " node init Error: " + this);
        } else {
            log.info("Initialized " + this.getClass().getSimpleName());
            initialized = true;
            active = true;
        }
    }

    /**
     * Do initialization
     *
     * @return {@code true} if it was initialized and {@code false} otherwise
     */
    protected abstract boolean doInit();

    @Override
    public String toString() {
        return this.getClass().getSimpleName().concat(":").concat(providerUrl.toFullStr());
    }
}

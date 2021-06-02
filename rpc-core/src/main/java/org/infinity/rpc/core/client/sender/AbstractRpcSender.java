package org.infinity.rpc.core.client.sender;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
@Slf4j
public abstract class AbstractRpcSender implements Sendable {
    protected volatile boolean       active          = false;
    protected          AtomicBoolean initialized     = new AtomicBoolean(false);
    protected          AtomicInteger processingCount = new AtomicInteger(0);
    protected          String        interfaceName;
    protected          Url           providerUrl;

    public AbstractRpcSender(String interfaceName, Url providerUrl) {
        this.interfaceName = interfaceName;
        this.providerUrl = providerUrl;
    }

    protected void init() {
        if (!initialized.compareAndSet(false, true)) {
            log.warn("RPC sender [{}] has already been initialized!", this);
            return;
        }
        boolean result = doInit();
        if (!result) {
            throw new RpcFrameworkException("Failed to initialize the RPC sender [" + this + "]!");
        }
        setActive(true);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public Url getProviderUrl() {
        return providerUrl;
    }

    @Override
    public Responseable sendRequest(Requestable request) {
        if (!active) {
            throw new RpcFrameworkException("No active RPC sender found for now!");
        }

        beforeSend();
        Responseable response = null;
        try {
            response = doSend(request);
            return response;
        } finally {
            afterSend(request, response);
        }
    }

    @Override
    public void destroy() {

    }

    /**
     * Do initialization
     *
     * @return {@code true} if it was initialized and {@code false} otherwise
     */
    protected abstract boolean doInit();

    protected abstract Responseable doSend(Requestable request);

    protected void beforeSend() {
        processingCount.incrementAndGet();
    }

    protected abstract void afterSend(Requestable request, Responseable response);
}

package org.infinity.rpc.core.client.sender;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractRequestSender implements Sendable {
    protected volatile boolean       active          = false;
    protected          String        interfaceName;
    protected          Url           providerUrl;
    protected          AtomicBoolean initialized     = new AtomicBoolean(false);
    protected          AtomicInteger processingCount = new AtomicInteger(0);

    public AbstractRequestSender(String interfaceName, Url providerUrl) {
        this.interfaceName = interfaceName;
        this.providerUrl = providerUrl;
    }

    protected void init() {
        if (!initialized.compareAndSet(false, true)) {
            log.warn("RPC sender [{}] has already been initialized!", this);
            return;
        }
        if (!doInit()) {
            throw new RpcFrameworkException("Failed to initialize the RPC request sender [" + this + "]!");
        }
        active = true;
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
            throw new RpcFrameworkException("No active RPC request sender found!");
        }
        Responseable response = null;
        try {
            beforeSend();
            response = doSend(request);
            return response;
        } finally {
            afterSend(request, response);
        }
    }

    @Override
    public void destroy() {
        active = false;
    }

    /**
     * Do initialization
     *
     * @return {@code true} if it was initialized and {@code false} otherwise
     */
    protected abstract boolean doInit();

    /**
     * Send RPC request
     *
     * @param request request
     * @return response
     */
    protected abstract Responseable doSend(Requestable request);

    /**
     * Before send handler
     */
    protected void beforeSend() {
        processingCount.incrementAndGet();
    }

    /**
     * After send handler
     *
     * @param request  request
     * @param response response
     */
    protected abstract void afterSend(Requestable request, Responseable response);
}

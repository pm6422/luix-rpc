package org.infinity.rpc.core.client.request;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorConstants;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
@Slf4j
public abstract class AbstractInvoker implements Invokable {
    protected volatile boolean       active          = false;
    protected          AtomicBoolean initialized     = new AtomicBoolean(false);
    protected          AtomicInteger processingCount = new AtomicInteger(0);
    protected          String        interfaceName;
    protected          Url           providerUrl;

    public AbstractInvoker(String interfaceName, Url providerUrl) {
        this.interfaceName = interfaceName;
        this.providerUrl = providerUrl;
    }

    protected void init() {
        if (!initialized.compareAndSet(false, true)) {
            log.warn("Provider invoker [{}] has already been initialized!", this.toString());
            return;
        }
        boolean result = doInit();
        if (!result) {
            throw new RpcFrameworkException("Failed to initialize the provider invoker [" + this + "]!",
                    RpcErrorConstants.FRAMEWORK_INIT_ERROR);
        } else {
            setActive(true);
        }
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
    public Responseable invoke(Requestable request) {
        if (!active) {
            throw new RpcFrameworkException("No active provider invoker found for now!");
        }

        beforeInvoke();
        Responseable response = null;
        try {
            response = doInvoke(request);
            return response;
        } finally {
            afterInvoke(request, response);
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

    protected abstract Responseable doInvoke(Requestable request);

    protected void beforeInvoke() {
        processingCount.incrementAndGet();
    }

    protected abstract void afterInvoke(Requestable request, Responseable response);
}

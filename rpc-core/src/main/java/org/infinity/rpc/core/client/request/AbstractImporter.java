package org.infinity.rpc.core.client.request;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
@Slf4j
public abstract class AbstractImporter implements Importable {
    protected volatile boolean       active          = false;
    protected          AtomicBoolean initialized     = new AtomicBoolean(false);
    protected          AtomicInteger processingCount = new AtomicInteger(0);
    protected          String        interfaceName;
    protected          Url           providerUrl;

    public AbstractImporter(String interfaceName, Url providerUrl) {
        this.interfaceName = interfaceName;
        this.providerUrl = providerUrl;
    }

    protected void init() {
        if (!initialized.compareAndSet(false, true)) {
            log.warn("Provider caller [{}] already has been initialized!", this.toString());
            return;
        }

        boolean result = doInit();
        if (!result) {
            throw new RpcFrameworkException("Failed to initialize the provider caller [" + this + "]!",
                    RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
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
    public Responseable call(Requestable request) {
        if (!active) {
            throw new RpcFrameworkException("No active provider caller found for now!");
        }

        increaseProcessingCount();
        Responseable response = null;
        try {
            response = doCall(request);
            return response;
        } finally {
            decreaseProcessingCount(request, response);
        }
    }

    @Override
    public void destroy() {

    }

    /**
     * Do initialization
     *
     * @return true: initialized, false: or else
     */
    protected abstract boolean doInit();

    protected abstract Responseable doCall(Requestable request);

    protected void increaseProcessingCount() {
        processingCount.incrementAndGet();
    }

    protected abstract void decreaseProcessingCount(Requestable request, Responseable response);
}

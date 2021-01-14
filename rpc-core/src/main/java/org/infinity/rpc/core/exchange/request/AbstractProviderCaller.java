package org.infinity.rpc.core.exchange.request;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @param <T>: The interface class of the provider
 */
@Slf4j
public abstract class AbstractProviderCaller<T> implements ProviderCaller<T> {
    protected volatile boolean       active          = false;
    protected          AtomicBoolean initialized     = new AtomicBoolean(false);
    protected          AtomicInteger processingCount = new AtomicInteger(0);
    protected          Class<T>      interfaceClass;
    protected          Url           providerUrl;

    public AbstractProviderCaller(Class<T> interfaceClass, Url providerUrl) {
        this.interfaceClass = interfaceClass;
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
            setAvailable(true);
        }
    }

    @Override
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setAvailable(boolean active) {
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

    protected abstract boolean doInit();

    @Override
    public void destroy() {

    }

    @Override
    public Responseable call(Requestable request) {
        if (!active) {
            throw new RpcFrameworkException("No active provider caller found for now!");
        }

        addProcessingCount();
        Responseable response = null;
        try {
            response = doCall(request);
            return response;
        } finally {
            reduceProcessingCount(request, response);
        }
    }

    protected abstract Responseable doCall(Requestable request);

    protected void addProcessingCount() {
        processingCount.incrementAndGet();
    }

    protected abstract void reduceProcessingCount(Requestable request, Responseable response);
}

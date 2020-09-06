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
    protected volatile boolean       available       = false;
    protected          AtomicBoolean initialized     = new AtomicBoolean(false);
    protected          AtomicInteger processingCount = new AtomicInteger(0);
    protected          Class<T>      interfaceClass;
    protected          Url           providerUrl;

    public AbstractProviderCaller(Class<T> interfaceClass, Url providerUrl) {
        this.interfaceClass = interfaceClass;
        this.providerUrl = providerUrl;
    }

    @Override
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public Url getProviderUrl() {
        return providerUrl;
    }

    @Override
    public void init() {
        if (!initialized.compareAndSet(false, true)) {
            log.warn("Provider caller {} has been already initialized!", this.getClass().getSimpleName());
            return;
        }

        boolean result = doInit();
        if (!result) {
            throw new RpcFrameworkException("Failed to initialize the provider caller!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        } else {
            log.info("Initialized the provider caller");
            available = true;
        }
    }

    protected abstract boolean doInit();

    @Override
    public void destroy() {

    }

    @Override
    public Responseable call(Requestable request) {
        if (!isAvailable()) {
            throw new RpcFrameworkException("No available provider caller found for now!");
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

package org.infinity.rpc.core.exchange.request;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class AbstractProtocolRequester<T> implements ProtocolRequester<T> {
    protected          AtomicBoolean initialized     = new AtomicBoolean(false);
    protected volatile boolean       available       = false;
    protected          Class<T>      interfaceClass;
    protected          Url           providerUrl;
    protected          AtomicInteger processingCount = new AtomicInteger(0);


    public AbstractProtocolRequester(Class<T> interfaceClass, Url providerUrl) {
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
            log.warn("Protocol requester {} has been already initialized!", this.getClass().getSimpleName());
            return;
        }

        boolean result = doInit();

        if (!result) {
            throw new RpcFrameworkException("Failed to initialize the protocol requester!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        } else {
            log.info("Initialized the protocol requester");
            available = true;
        }
    }

    protected abstract boolean doInit();

    @Override
    public void destroy() {

    }

    protected void increaseProcessingCount() {
        processingCount.incrementAndGet();
    }

    protected abstract void decreaseProcessingCount(Requestable request, Responseable response);

    @Override
    public Responseable call(Requestable request) {
        if (!isAvailable()) {
            throw new RpcFrameworkException("No available protocol requester found for now!");
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

    protected abstract Responseable doCall(Requestable request);
}

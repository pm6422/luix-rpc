package org.infinity.rpc.core.config.spring.server.exporter;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapper;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;

@Slf4j
public abstract class AbstractExporter<T> implements Exportable<T> {
    protected          ProviderWrapper<T> providerWrapper;
    protected volatile boolean            init      = false;
    protected volatile boolean            available = false;

    public AbstractExporter(ProviderWrapper<T> providerWrapper) {
        this.providerWrapper = providerWrapper;
    }

    @Override
    public ProviderWrapper<T> getProviderWrapper() {
        return providerWrapper;
    }

    @Override
    public synchronized void init() {
        if (init) {
            log.warn(this.getClass().getSimpleName() + " node already init: " + desc());
            return;
        }

        boolean result = doInit();

        if (!result) {
            log.error(this.getClass().getSimpleName() + " node init Error: " + desc());
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " node init Error: " + desc(), RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        } else {
            log.info(this.getClass().getSimpleName() + " node init Success: " + desc());
            init = true;
            available = true;
        }
    }

    protected abstract boolean doInit();

    @Override
    public String desc() {
        return "[" + this.getClass().getSimpleName() + "] url=" + providerWrapper.getUrl();
    }
}

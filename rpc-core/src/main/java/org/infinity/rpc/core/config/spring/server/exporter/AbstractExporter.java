package org.infinity.rpc.core.config.spring.server.exporter;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.server.stub.ProviderStub;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;

@Slf4j
public abstract class AbstractExporter<T> implements Exportable<T> {
    protected          ProviderStub<T> providerStub;
    protected volatile boolean         init      = false;
    protected volatile boolean            available = false;

    public AbstractExporter(ProviderStub<T> providerStub) {
        this.providerStub = providerStub;
    }

    @Override
    public ProviderStub<T> getProviderStub() {
        return providerStub;
    }

    @Override
    public synchronized void init() {
        if (init) {
            log.warn(this.getClass().getSimpleName() + " node already init: " + toString());
            return;
        }

        boolean result = doInit();

        if (!result) {
            log.error(this.getClass().getSimpleName() + " node init Error: " + toString());
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " node init Error: " + toString(), RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        } else {
            log.info(this.getClass().getSimpleName() + " node init Success: " + toString());
            init = true;
            available = true;
        }
    }

    /**
     * Do initialization
     *
     * @return true: initialized successfully, false: or else
     */
    protected abstract boolean doInit();

    @Override
    public String toString() {
        return this.getClass().getSimpleName().concat(":").concat(providerStub.getUrl().toFullStr());
    }
}

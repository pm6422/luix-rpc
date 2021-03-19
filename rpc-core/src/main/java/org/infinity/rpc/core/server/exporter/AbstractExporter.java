package org.infinity.rpc.core.server.exporter;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.server.stub.ProviderStub;

@Slf4j
public abstract class AbstractExporter<T> implements Exportable<T> {
    protected          ProviderStub<T> providerStub;
    protected volatile boolean         initialized = false;
    protected volatile boolean         active      = false;

    public AbstractExporter(ProviderStub<T> providerStub) {
        this.providerStub = providerStub;
    }

    @Override
    public ProviderStub<T> getProviderStub() {
        return providerStub;
    }

    @Override
    public synchronized void init() {
        if (initialized) {
            log.warn(this.getClass().getSimpleName() + " node already init: " + toString());
            return;
        }

        boolean result = doInit();

        if (!result) {
            log.error(this.getClass().getSimpleName() + " node init Error: " + toString());
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " node init Error: " + toString(), RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        } else {
            log.info(this.getClass().getSimpleName() + " node init Success: " + toString());
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
        return this.getClass().getSimpleName().concat(":").concat(providerStub.getUrl().toFullStr());
    }
}

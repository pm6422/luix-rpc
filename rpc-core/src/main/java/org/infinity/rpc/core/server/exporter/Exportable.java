package org.infinity.rpc.core.server.exporter;

import org.infinity.rpc.core.server.stub.ProviderStub;

/**
 * todo: merge with ProviderCallable
 *
 * @param <T>
 */
public interface Exportable<T> {
    /**
     * Get provider stub
     * @return provider stub
     */
    ProviderStub<T> getProviderStub();

    /**
     * Initialize
     */
    void init();

    /**
     * Check whether it is available
     *
     * @return true: available, false: unavailable
     */
    boolean isActive();

    /**
     * Undo exported provider
     */
    void undoExport();

    /**
     * Do some cleanup task
     */
    void destroy();
}

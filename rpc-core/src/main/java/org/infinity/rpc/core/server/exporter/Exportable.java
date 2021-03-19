package org.infinity.rpc.core.server.exporter;

import org.infinity.rpc.core.server.stub.ProviderStub;

/**
 * @param <T>
 */
public interface Exportable<T> {
    /**
     * Get provider stub
     *
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
     * @return {@code true} if it was active and {@code false} otherwise
     */
    boolean isActive();

    /**
     * Cancel exported provider
     */
    void cancelExport();

    /**
     * Do some cleanup task
     */
    void destroy();
}

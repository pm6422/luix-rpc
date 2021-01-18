package org.infinity.rpc.core.config.spring.server.exporter;

import org.infinity.rpc.core.config.spring.server.stub.ProviderStub;

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
    boolean isAvailable();

    /**
     * Remove exported provider
     */
    void cancelExport();

    /**
     * Do some cleanup task
     */
    void destroy();
}

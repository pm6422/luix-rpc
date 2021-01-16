package org.infinity.rpc.core.config.spring.server.exporter;

import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapper;

/**
 * todo: merge with ProviderCallable
 *
 * @param <T>
 */
public interface Exportable<T> {
    /**
     * Get provider wrapper
     * @return provider wrapper
     */
    ProviderWrapper<T> getProviderWrapper();

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

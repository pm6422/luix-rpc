package org.infinity.luix.core.server.exporter;

import org.infinity.luix.core.url.Url;

public interface Exportable {
    /**
     * Get provider URL
     *
     * @return provider URL
     */
    Url getProviderUrl();

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

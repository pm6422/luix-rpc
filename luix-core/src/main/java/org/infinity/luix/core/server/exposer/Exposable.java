package org.infinity.luix.core.server.exposer;

import org.infinity.luix.core.url.Url;

public interface Exposable {
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
     * Cancel exposed provider
     */
    void cancelExpose();

    /**
     * Do some cleanup task
     */
    void destroy();
}

package org.infinity.luix.core.server.exposer;

import org.infinity.luix.core.url.Url;

public interface ProviderExposable {
    /**
     * Get provider URL
     *
     * @return provider URL
     */
    Url getProviderUrl();

    /**
     * Expose provider
     */
    void expose();

    /**
     * Cancel exposed provider
     */
    void cancelExpose();

    /**
     * Check whether it is available
     *
     * @return {@code true} if it was active and {@code false} otherwise
     */
    boolean isActive();


    /**
     * Do some cleanup task
     */
    void destroy();
}

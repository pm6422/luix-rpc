package org.infinity.rpc.core.exchange.response;

import org.infinity.rpc.core.url.Url;

/**
 * todo: merge with ProviderCallable
 *
 * @param <T>
 */
public interface Exportable<T> {
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

    String desc();

    /**
     * Do some cleanup task
     */
    void destroy();

    Url getProviderUrl();
}

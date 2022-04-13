package com.luixtech.rpc.core.client.ratelimit;

import com.luixtech.utilities.serviceloader.ServiceLoader;
import com.luixtech.utilities.serviceloader.annotation.Spi;
import com.luixtech.utilities.serviceloader.annotation.SpiScope;

/**
 * Rate limiter used to control API processing rates in a high-concurrency system
 * in order to avoid errors and performance issues.
 */
@Spi(scope = SpiScope.SINGLETON)
public interface RateLimiter {

    /**
     * Initializes RateLimiter's state and stores config
     *
     * @param permitsPerSecond permits per second
     */
    void create(long permitsPerSecond);

    /**
     * Updates RateLimiter's state and stores config
     *
     * @param permitsPerSecond permits per second
     */
    void update(long permitsPerSecond);


    /**
     * Acquires a permit only if one is available at the time of invocation.
     *
     * @return {@code true} if a permit was acquired and {@code false} otherwise
     */
    boolean tryAcquire();

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static RateLimiter getInstance(String name) {
        return ServiceLoader.forClass(RateLimiter.class).load(name);
    }
}

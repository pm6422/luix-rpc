package org.infinity.rpc.core.client.ratelimit.impl;

import org.infinity.rpc.core.client.ratelimit.RateLimiter;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.infinity.rpc.core.constant.ConsumerConstants.RATE_LIMITER_GUAVA;

@SpiName(RATE_LIMITER_GUAVA)
public class GuavaRateLimiter implements RateLimiter {

    private       com.google.common.util.concurrent.RateLimiter rateLimiter;
    /**
     * Indicates whether the limiter already initialized or not
     */
    private final AtomicBoolean                                 initialized = new AtomicBoolean(false);

    @Override
    public void init(long permitsPerSecond) {
        rateLimiter = com.google.common.util.concurrent.RateLimiter.create(permitsPerSecond);
        initialized.set(true);
    }

    @Override
    public void update(long permitsPerSecond) {
        if (!initialized.get()) {
            throw new RpcConfigurationException("Please initialize it before use!");
        }
        rateLimiter.setRate(permitsPerSecond);
    }

    @Override
    public boolean tryAcquire() {
        if (!initialized.get()) {
            throw new RpcConfigurationException("Please initialize it before use!");
        }
        return rateLimiter.tryAcquire();
    }
}

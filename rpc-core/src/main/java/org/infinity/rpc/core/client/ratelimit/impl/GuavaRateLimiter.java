package org.infinity.rpc.core.client.ratelimit.impl;

import com.google.common.annotations.Beta;
import org.infinity.rpc.core.client.ratelimit.RateLimiter;
import org.infinity.rpc.core.exception.impl.RpcConfigurationException;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.ConsumerConstants.RATE_LIMITER_GUAVA;

@SpiName(RATE_LIMITER_GUAVA)
@Beta
public class GuavaRateLimiter implements RateLimiter {

    private com.google.common.util.concurrent.RateLimiter rateLimiter;

    @Override
    public void create(long permitsPerSecond) {
        rateLimiter = com.google.common.util.concurrent.RateLimiter.create(permitsPerSecond);
    }

    @Override
    public void update(long permitsPerSecond) {
        if (rateLimiter == null) {
            throw new RpcConfigurationException("Please initialize it before use!");
        }
        rateLimiter.setRate(permitsPerSecond);
    }

    @Override
    public boolean tryAcquire() {
        if (rateLimiter == null) {
            throw new RpcConfigurationException("Please initialize it before use!");
        }
        return rateLimiter.tryAcquire();
    }
}

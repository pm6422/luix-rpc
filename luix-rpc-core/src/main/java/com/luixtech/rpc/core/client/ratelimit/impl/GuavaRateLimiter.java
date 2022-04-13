package com.luixtech.rpc.core.client.ratelimit.impl;

import com.google.common.annotations.Beta;
import com.luixtech.rpc.core.client.ratelimit.RateLimiter;
import com.luixtech.rpc.core.exception.impl.RpcConfigException;
import com.luixtech.utilities.serviceloader.annotation.SpiName;

import static com.luixtech.rpc.core.constant.ConsumerConstants.RATE_LIMITER_GUAVA;

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
            throw new RpcConfigException("Please initialize it before use!");
        }
        rateLimiter.setRate(permitsPerSecond);
    }

    @Override
    public boolean tryAcquire() {
        if (rateLimiter == null) {
            throw new RpcConfigException("Please initialize it before use!");
        }
        return rateLimiter.tryAcquire();
    }
}

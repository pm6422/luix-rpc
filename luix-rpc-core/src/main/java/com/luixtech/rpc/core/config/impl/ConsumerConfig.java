package com.luixtech.rpc.core.config.impl;

import com.luixtech.rpc.core.client.ratelimit.RateLimiter;
import com.luixtech.rpc.core.constant.ConsumerConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotEmpty;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ConsumerConfig extends ServiceConfig {
    public static final String  PREFIX           = "consumer";
    /**
     * Service provider invoker
     */
    @NotEmpty
    private             String  invoker          = ConsumerConstants.INVOKER_VAL_DEFAULT;
    /**
     * Fault tolerance strategy
     */
    @NotEmpty
    private             String  faultTolerance   = ConsumerConstants.FAULT_TOLERANCE_VAL_DEFAULT;
    /**
     * Cluster loadBalancer implementation
     */
    @NotEmpty
    private             String  loadBalancer     = ConsumerConstants.LOAD_BALANCER_VAL_DEFAULT;
    /**
     * Consumer proxy factory
     */
    @NotEmpty
    private             String  proxyFactory     = ConsumerConstants.PROXY_VAL_DEFAULT;
    /**
     * Indicates whether rate limit enabled or not
     */
    private             boolean limitRate;
    /**
     * Permits per second of rate limit
     */
    private             long    permitsPerSecond = 100L;

    public void init() {
        checkIntegrity();
        checkValidity();
        initRateLimiter();
        log.info("Luix consumer configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {

    }

    @Override
    public void checkValidity() {

    }

    private void initRateLimiter() {
        RateLimiter.getInstance(ConsumerConstants.RATE_LIMITER_GUAVA).create(permitsPerSecond);
    }
}

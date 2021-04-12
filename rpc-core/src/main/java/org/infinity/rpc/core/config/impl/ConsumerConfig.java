package org.infinity.rpc.core.config.impl;

import lombok.Data;
import org.infinity.rpc.core.client.ratelimit.RateLimiter;

import javax.validation.constraints.NotEmpty;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;

@Data
public class ConsumerConfig extends ServiceConfig {
    public static final String  PREFIX           = "consumer";
    /**
     * Cluster implementation
     */
    @NotEmpty
    private             String  cluster          = CLUSTER_VAL_DEFAULT;
    /**
     * Fault tolerance strategy
     */
    @NotEmpty
    private             String  faultTolerance   = FAULT_TOLERANCE_VAL_FAILOVER;
    /**
     * Cluster loadBalancer implementation
     */
    @NotEmpty
    private             String  loadBalancer     = LOAD_BALANCER_VAL_RANDOM;
    /**
     * Consumer proxy factory
     */
    @NotEmpty
    private             String  proxyFactory     = PROXY_VAL_JDK;
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
    }

    @Override
    public void checkIntegrity() {

    }

    @Override
    public void checkValidity() {

    }

    private void initRateLimiter() {
        RateLimiter.getInstance(RATE_LIMITER_GUAVA).create(permitsPerSecond);
    }
}

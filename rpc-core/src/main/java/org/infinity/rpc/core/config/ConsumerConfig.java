package org.infinity.rpc.core.config;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;

@Data
public class ConsumerConfig extends ServiceConfig {
    public static final String PREFIX         = "consumer";
    /**
     * Cluster implementation
     */
    @NotEmpty
    private             String cluster        = CLUSTER_VAL_DEFAULT;
    /**
     * Fault tolerance strategy
     */
    @NotEmpty
    private             String faultTolerance = FAULT_TOLERANCE_VAL_FAILOVER;
    /**
     * Cluster loadBalancer implementation
     */
    @NotEmpty
    private             String loadBalancer   = LOAD_BALANCER_VAL_RANDOM;
    /**
     * Consumer proxy factory
     */
    @NotEmpty
    private             String proxyFactory   = PROXY_FACTORY_VAL_JDK;

    public void init() {
    }
}

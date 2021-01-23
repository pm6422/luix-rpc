package org.infinity.rpc.core.config;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;

@Data
public class ConsumerConfig extends ServiceConfig {
    /**
     * Cluster implementation
     */
    @NotEmpty
    private String cluster        = CLUSTER_DEFAULT_VALUE;
    /**
     * Fault tolerance strategy
     */
    @NotEmpty
    private String faultTolerance = FAULT_TOLERANCE_DEFAULT_VALUE;
    /**
     * Cluster loadBalancer implementation
     */
    @NotEmpty
    private String loadBalancer   = LOAD_BALANCER_DEFAULT_VALUE;
}

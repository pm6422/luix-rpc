package org.infinity.rpc.spring.boot.config;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;

@Data
@Validated
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

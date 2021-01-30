package org.infinity.rpc.core.constant;

/**
 * All the attribute names of {@link org.infinity.rpc.core.client.annotation.Consumer}
 */
public interface ConsumerConstants extends ServiceConstants {
    String CLUSTER                       = "cluster";
    String CLUSTER_DEFAULT_VALUE         = "default";
    String FAULT_TOLERANCE               = "faultTolerance";
    String FAULT_TOLERANCE_DEFAULT_VALUE = "failover";
    String LOAD_BALANCER                 = "loadBalancer";
    String LOAD_BALANCER_DEFAULT_VALUE   = "random";
    String DIRECT_URLS                   = "directUrls";
}

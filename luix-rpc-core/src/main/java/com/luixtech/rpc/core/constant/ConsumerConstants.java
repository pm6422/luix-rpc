package com.luixtech.rpc.core.constant;

import com.luixtech.rpc.core.client.annotation.RpcConsumer;

/**
 * All the attribute names of {@link RpcConsumer}
 */
public interface ConsumerConstants extends ServiceConstants {
    String INVOKER                       = "invoker";
    String INVOKER_VAL_DEFAULT           = "default";
    String FAULT_TOLERANCE               = "faultTolerance";
    String FAULT_TOLERANCE_VAL_FAILOVER  = "failover";
    String FAULT_TOLERANCE_VAL_FAILFAST  = "failfast";
    String FAULT_TOLERANCE_VAL_BROADCAST = "broadcast";
    String FAULT_TOLERANCE_VAL_DEFAULT   = FAULT_TOLERANCE_VAL_FAILOVER;
    String LOAD_BALANCER                 = "loadBalancer";
    String LOAD_BALANCER_VAL_RANDOM      = "random";
    String LOAD_BALANCER_VAL_DEFAULT     = LOAD_BALANCER_VAL_RANDOM;
    String PROVIDER_ADDRESSES            = "providerAddresses";
    String LIMIT_RATE                    = "limitRate";
    String RATE_LIMITER_GUAVA            = "guava";
    String PROXY                         = "proxy";
    String PROXY_VAL_JDK                 = "jdk";
    String PROXY_VAL_JAVASSIST           = "javassist";
    String PROXY_VAL_DEFAULT             = PROXY_VAL_JDK;
}

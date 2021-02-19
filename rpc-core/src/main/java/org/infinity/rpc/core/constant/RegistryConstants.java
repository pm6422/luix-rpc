package org.infinity.rpc.core.constant;

import java.util.concurrent.TimeUnit;

public interface RegistryConstants {
    String REGISTRY                      = "registry";
    String REGISTRY_DEFAULT_VALUE        = "zookeeper";
    String REGISTRY_VALUE_DIRECT         = "direct";
    String CONNECT_TIMEOUT               = "connectTimeout";
    int    CONNECT_TIMEOUT_DEFAULT_VALUE = Math.toIntExact(TimeUnit.SECONDS.toMillis(1));
    String SESSION_TIMEOUT               = "sessionTimeout";
    int    SESSION_TIMEOUT_DEFAULT_VALUE = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
    String RETRY_INTERVAL                = "retryInterval";
    int    RETRY_INTERVAL_DEFAULT_VALUE  = Math.toIntExact(TimeUnit.SECONDS.toMillis(30));
}

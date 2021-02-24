package org.infinity.rpc.core.constant;

import java.util.concurrent.TimeUnit;

public interface RegistryConstants {
    String  REGISTRY                    = "registry";
    String  REGISTRY_VAL_ZOOKEEPER      = "zookeeper";
    String  REGISTRY_VAL_DIRECT         = "direct";
    String  CONNECT_TIMEOUT             = "connectTimeout";
    int     CONNECT_TIMEOUT_VAL_DEFAULT = Math.toIntExact(TimeUnit.SECONDS.toMillis(1));
    String  SESSION_TIMEOUT             = "sessionTimeout";
    int     SESSION_TIMEOUT_VAL_DEFAULT = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));
    String  RETRY_INTERVAL              = "retryInterval";
    int     RETRY_INTERVAL_VAL_DEFAULT  = Math.toIntExact(TimeUnit.SECONDS.toMillis(30));
    String  THROW_EXCEPTION             = "throwException";
    boolean THROW_EXCEPTION_VAL_DEFAULT = true;
}

package org.infinity.rpc.core.constant;

import org.infinity.rpc.core.client.annotation.RpcConsumer;
import org.infinity.rpc.core.server.annotation.RpcProvider;

/**
 * All the attribute names of
 * {@link RpcConsumer}
 * {@link RpcProvider}
 */
public interface ServiceConstants {
    String BEAN_NAME       = "beanName";
    String INTERFACE_NAME  = "interfaceName";
    String INTERFACE_CLASS = "interfaceClass";

    String FORM                        = "form";
    String VERSION                     = "version";
    String HEALTH_CHECKER              = "healthChecker";
    String HEALTH_CHECKER_VAL_DEFAULT  = "default";
    String RETRY_COUNT                 = "retryCount";
    int    RETRY_COUNT_VAL_DEFAULT     = 0;
    String REQUEST_TIMEOUT             = "requestTimeout";
    int    REQUEST_TIMEOUT_VAL_DEFAULT = 500;
    String MAX_PAYLOAD                 = "maxPayload";
    /**
     * 8M bytes
     */
    int    MAX_PAYLOAD_VAL_DEFAULT     = 8 * 1024 * 1024;
    String METHOD_CONFIG               = "mConfig";
}

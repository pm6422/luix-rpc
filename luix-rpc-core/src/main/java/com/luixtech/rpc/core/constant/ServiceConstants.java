package com.luixtech.rpc.core.constant;

import com.luixtech.rpc.core.client.annotation.RpcConsumer;
import com.luixtech.rpc.core.server.annotation.RpcProvider;

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
    String RETRY_COUNT                 = "retryCount";
    int    RETRY_COUNT_VAL_DEFAULT     = 0;
    String REQUEST_TIMEOUT             = "requestTimeout";
    /**
     * timeout超时参数设置，通常是这么设置的，对于要调用的系统要看看他平时调用要多久能返回，然后比正常的耗时设置的多个50%就可以了，
     * 比如平时一般正常在100~200ms，偶尔高峰会在500ms，那你设置个timeout=800ms或者1s其实都可以。
     */
    int    REQUEST_TIMEOUT_VAL_DEFAULT = 500;
    String MAX_PAYLOAD                 = "maxPayload";
    /**
     * 8M bytes
     */
    int    MAX_PAYLOAD_VAL_DEFAULT     = 8 * 1024 * 1024;
}

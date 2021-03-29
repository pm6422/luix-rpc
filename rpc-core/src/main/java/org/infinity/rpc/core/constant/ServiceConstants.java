package org.infinity.rpc.core.constant;

/**
 * All the attribute names of
 * {@link org.infinity.rpc.core.client.annotation.Consumer}
 * {@link org.infinity.rpc.core.server.annotation.Provider}
 */
public interface ServiceConstants {
    String BEAN_NAME       = "beanName";
    String INTERFACE_NAME  = "interfaceName";
    String INTERFACE_CLASS = "interfaceClass";

    String FORM                        = "form";
    String FORM_VAL_DEFAULT            = "default";
    String VERSION                     = "version";
    String VERSION_VAL_DEFAULT         = "1.0.0";
    String HEALTH_CHECKER              = "healthChecker";
    String HEALTH_CHECKER_VAL_DEFAULT  = "default";
    String MAX_RETRIES                 = "maxRetries";
    int    MAX_RETRIES_VAL_DEFAULT     = 0;
    String REQUEST_TIMEOUT             = "requestTimeout";
    int    REQUEST_TIMEOUT_VAL_DEFAULT = 500;
    String MAX_PAYLOAD                 = "maxPayload";
    /**
     * 8M bytes
     */
    int    MAX_PAYLOAD_VAL_DEFAULT     = 8 * 1024 * 1024;
}

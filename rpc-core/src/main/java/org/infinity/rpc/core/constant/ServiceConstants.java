package org.infinity.rpc.core.constant;

/**
 * All the attribute names of
 * {@link org.infinity.rpc.core.client.annotation.Consumer}
 * {@link org.infinity.rpc.core.server.annotation.Provider}
 */
public interface ServiceConstants {
    String INTERFACE_NAME  = "interfaceName";
    String INTERFACE_CLASS = "interfaceClass";
    String GENERIC         = "generic";

    String  GROUP                              = "group";
    String  GROUP_DEFAULT_VALUE                = "default";
    String  VERSION                            = "version";
    String  VERSION_DEFAULT_VALUE              = "1.0.0";
    String  CHECK_HEALTH                       = "checkHealth";
    boolean CHECK_HEALTH_DEFAULT_VALUE         = true;
    String  CHECK_HEALTH_FACTORY               = "checkHealthFactory";
    String  CHECK_HEALTH_FACTORY_DEFAULT_VALUE = "default";
    String  MAX_RETRIES                        = "maxRetries";
    int     MAX_RETRIES_DEFAULT_VALUE          = 0;
    String  REQUEST_TIMEOUT                    = "requestTimeout";
    int     REQUEST_TIMEOUT_DEFAULT_VALUE      = 500;
}

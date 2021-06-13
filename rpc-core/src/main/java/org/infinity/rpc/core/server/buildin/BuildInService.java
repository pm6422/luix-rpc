package org.infinity.rpc.core.server.buildin;

import org.infinity.rpc.core.config.impl.ApplicationConfig;

public interface BuildInService {
    String METHOD_GET_APPLICATION_CONFIG = "getApplicationConfig";
    String METHOD_GET_HEALTH             = "getHealth";
    String METHOD_GET_SYSTEM_TIME        = "getSystemTime";

    /**
     * Get application configuration
     *
     * @return application configuration
     */
    ApplicationConfig getApplicationConfig();

    /**
     * Get health status
     *
     * @return OK status
     */
    String getHealth();

    /**
     * Get system time
     *
     * @return system time of the server
     */
    String getSystemTime();
}

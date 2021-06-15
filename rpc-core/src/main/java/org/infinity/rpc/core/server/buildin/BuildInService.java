package org.infinity.rpc.core.server.buildin;

import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.server.stub.MethodData;

import java.util.List;

public interface BuildInService {
    String METHOD_GET_APPLICATION_CONFIG = "getApplicationConfig";
    String METHOD_GET_HEALTH             = "getHealth";
    String METHOD_GET_SYSTEM_TIME        = "getSystemTime";
    String METHOD_GET_METHODS            = "getMethods";

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

    /**
     * Get all methods of provider interface
     *
     * @param interfaceClassName provider interface class name
     * @param form               form
     * @param version            version
     * @return methods
     */
    List<MethodData> getMethods(String interfaceClassName, String form, String version);
}

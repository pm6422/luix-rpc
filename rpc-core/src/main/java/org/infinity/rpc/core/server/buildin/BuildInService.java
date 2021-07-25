package org.infinity.rpc.core.server.buildin;

import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.server.stub.MethodMeta;

import java.util.List;

public interface BuildInService {
    String METHOD_GET_APPLICATION_INFO = "getApplicationInfo";
    String METHOD_GET_SERVER_INFO      = "getServerInfo";
    String METHOD_CHECK_HEALTH         = "checkHealth";
    String METHOD_GET_METHODS          = "getMethods";
    String METHOD_ACTIVATE             = "activate";
    String METHOD_DEACTIVATE           = "deactivate";
    String METHOD_REREGISTER           = "reregister";

    /**
     * Get application information
     *
     * @return application information
     */
    ApplicationConfig getApplicationInfo();

    /**
     * Get server information
     *
     * @return server information
     */
    ServerInfo getServerInfo();

    /**
     * Check health status
     *
     * @param interfaceClassName provider interface class name
     * @param form               form
     * @param version            version
     * @return OK status
     */
    String checkHealth(String interfaceClassName, String form, String version);

    /**
     * Get all methods of provider interface
     *
     * @param interfaceClassName provider interface class name
     * @param form               form
     * @param version            version
     * @return methods
     */
    List<MethodMeta> getMethods(String interfaceClassName, String form, String version);

    /**
     * Activate the provider service
     *
     * @param interfaceClassName provider interface class name
     * @param form               form
     * @param version            version
     */
    void activate(String interfaceClassName, String form, String version);

    /**
     * Deactivate the provider service
     *
     * @param interfaceClassName provider interface class name
     * @param form               form
     * @param version            version
     */
    void deactivate(String interfaceClassName, String form, String version);

    /**
     * Re-register url
     *
     * @param urlStr url
     */
    void reregister(String urlStr);
}

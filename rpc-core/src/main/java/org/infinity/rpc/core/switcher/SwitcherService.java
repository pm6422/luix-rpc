package org.infinity.rpc.core.switcher;


import org.infinity.rpc.utilities.spi.annotation.ServiceInstanceScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

@Spi(scope = ServiceInstanceScope.SINGLETON)
public interface SwitcherService {
    String REGISTRY_HEARTBEAT_SWITCHER = "feature.configserver.heartbeat";

    /**
     * Get switcher by name
     *
     * @param name name of switcher
     * @return switcher
     */
    Switcher getSwitcher(String name);

    /**
     * Get all switchers
     *
     * @return switchers
     */
    List<Switcher> getAllSwitchers();

    /**
     * Set initial value to specified switcher
     *
     * @param name         name of switcher
     * @param initialValue initial value
     */
    void initSwitcher(String name, boolean initialValue);

    /**
     * Assign value
     *
     * @param name  name of switcher
     * @param value new value
     */
    void setValue(String name, boolean value);

    /**
     * Check if the switcher is on or not
     *
     * @param name switcher name
     * @return on or not
     */
    boolean isOn(String name);

    /**
     * Check if the switcher is on or not, return the value if switcher exist,
     * set the switcher with default value and return the default value if switcher does not exist
     *
     * @param name         switcher name
     * @param defaultValue default value
     * @return on or not
     */
    boolean isOn(String name, boolean defaultValue);

    /**
     * Register a listener for the specified switcher
     *
     * @param name     switcher name
     * @param listener listener
     */
    void registerListener(String name, SwitcherListener listener);

    /**
     * Unregister a listener
     *
     * @param name     switcher name
     * @param listener the listener to be unregistered
     */
    void unregisterListener(String name, SwitcherListener listener);

    /**
     * Unregister all listeners specified by name
     * @param name switcher name
     */
    void unregisterListeners(String name);
}

package org.infinity.rpc.core.switcher;


import org.infinity.rpc.utilities.spi.annotation.Scope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.util.List;

@Spi(scope = Scope.SINGLETON)
public interface SwitcherService {
    String REGISTRY_HEARTBEAT_SWITCHER = "feature.configserver.heartbeat";

    Switcher getSwitcher(String name);

    List<Switcher> getAllSwitchers();

    void initSwitcher(String name, boolean initialValue);

    boolean isOpen(String name);

    /**
     * Check if the switcher is on or not, return the value if switcher exist,
     * set the switcher with default value and return the default value if switcher does not exist
     *
     * @param name         switcher name
     * @param defaultValue default value
     * @return open or not
     */
    boolean isOpen(String name, boolean defaultValue);

    void setValue(String name, boolean value);

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
     * @param listener the listener to be unregistered, null for all listeners for this name
     */
    void unRegisterListener(String name, SwitcherListener listener);
}

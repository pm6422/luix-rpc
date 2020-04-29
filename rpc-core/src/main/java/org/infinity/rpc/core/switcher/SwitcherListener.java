package org.infinity.rpc.core.switcher;

/**
 * Listener to monitor the value changes
 */
public interface SwitcherListener {

    void onSubscribe(String name, Boolean value);
}

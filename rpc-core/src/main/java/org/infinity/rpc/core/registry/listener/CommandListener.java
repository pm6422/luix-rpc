package org.infinity.rpc.core.registry.listener;

import org.infinity.rpc.core.registry.Url;

/**
 * Listener of command used to handle the subscribed event
 */
public interface CommandListener {

    void onSubscribe(Url clientUrl, String commandString);
}
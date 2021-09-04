package org.infinity.luix.core.registry.listener;

import org.infinity.luix.core.url.Url;

/**
 * Listener of command used to handle the subscribed event
 */
public interface CommandListener {

    void onNotify(Url consumerUrl, String commandString);
}
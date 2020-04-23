package org.infinity.rpc.core.registry.listener;

import org.infinity.rpc.core.registry.Url;

public interface CommandListener {

    void notifyCommand(Url refUrl, String commandString);

}
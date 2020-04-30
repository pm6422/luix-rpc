package org.infinity.rpc.core.registry.listener;

import org.infinity.rpc.core.registry.Url;

public interface CommandListener {

    void onSubscribe(Url refUrl, String commandString);

}
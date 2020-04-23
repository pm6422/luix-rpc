package org.infinity.rpc.core.registry;

import org.infinity.rpc.core.registry.listener.NotifyListener;

import java.util.List;

public interface Subscribable {

    void subscribe(Url url, NotifyListener listener);

    void unsubscribe(Url url, NotifyListener listener);

    List<Url> discover(Url url);
}
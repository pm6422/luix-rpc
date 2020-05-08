package org.infinity.rpc.core.registry;

import java.util.Collection;

public interface Registrable {

    void register(Url url);

    void unregister(Url url);

    void activate(Url url);

    void deactivate(Url Url);

    Collection<Url> getRegisteredProviderUrls();
}

package org.infinity.luix.core.exchange.endpoint;

public interface EndpointManager {

    void init();

    void addEndpoint(Endpoint endpoint);

    void removeEndpoint(Endpoint endpoint);

    void destroy();

}

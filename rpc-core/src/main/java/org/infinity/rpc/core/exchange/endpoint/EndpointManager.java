package org.infinity.rpc.core.exchange.endpoint;

public interface EndpointManager {

    void init();

    void addEndpoint(Endpoint endpoint);

    void removeEndpoint(Endpoint endpoint);

    void destroy();

}

package org.infinity.rpc.core.exchange.transport.endpoint;

public interface EndpointManager {

    void init();

    void addEndpoint(Endpoint endpoint);

    void removeEndpoint(Endpoint endpoint);

    void destroy();

}

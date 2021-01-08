package org.infinity.rpc.core.exchange.transport.endpoint;

public interface EndpointManager {

    void init();

    void destroy();

    void addEndpoint(Endpoint endpoint);

    void removeEndpoint(Endpoint endpoint);

}

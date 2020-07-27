package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.registry.Url;

/**
 * The initiator of the RPC request
 */
public interface Requester<T> {

    /**
     * @return
     */
    Class<T> getInterfaceClass();

    /**
     * @param available
     */
    void setAvailable(boolean available);

    /**
     * @return
     */
    boolean isAvailable();

    /**
     * @return
     */
    Url getUrl();

    /**
     * @return
     */
    Url getProviderUrl();

    /**
     *
     */
    void init();

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable call(Requestable request);
}

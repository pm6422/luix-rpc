package com.luixtech.rpc.core.client.sender;

import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.rpc.core.url.Url;

/**
 * RPC sender used to call service provider
 */
public interface Sendable {

    /**
     * Get provider url
     *
     * @return provider url
     */
    Url getProviderUrl();

    /**
     * Check whether the RPC sender is active
     *
     * @return {@code true} if it was active and {@code false} otherwise
     */
    boolean isActive();

    /**
     * Send RPC request
     *
     * @param request request object
     * @return response object
     */
    Responseable sendRequest(Requestable request);

    /**
     * Do some cleanup task
     */
    void destroy();
}

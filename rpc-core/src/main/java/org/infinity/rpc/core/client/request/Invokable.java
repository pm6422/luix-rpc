package org.infinity.rpc.core.client.request;

import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

/**
 * RPC invoker used to call provider
 */
public interface Invokable {

    /**
     * Get provider url
     *
     * @return provider url
     */
    Url getProviderUrl();

    /**
     * Check whether the invoker is active
     *
     * @return true: active, false: inactive
     */
    boolean isActive();

    /**
     * Invoke provider
     *
     * @param request request object
     * @return response object
     */
    Responseable invoke(Requestable request);

    /**
     * Do some cleanup task
     */
    void destroy();
}

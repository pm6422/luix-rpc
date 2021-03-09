package org.infinity.rpc.core.client.cluster;

import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;

/**
 * todo: merge to ProviderCluster
 */
public interface ProviderCallable {
    /**
     * Initialize
     */
    void init();

    /**
     * Check whether it is available
     *
     * @return true: available, false: unavailable
     */
    boolean isActive();

    /**
     * Initiate a RPC call
     *
     * @param request request object
     * @return response object
     */
    Responseable call(Requestable request);
}

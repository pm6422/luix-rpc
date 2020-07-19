package org.infinity.rpc.core.exchange;

import java.util.Map;

public interface Requestable<T> extends Exchangable {
    /**
     * Provider interface name
     *
     * @return
     */
    String getInterfaceName();

    /**
     * Provider method name
     *
     * @return
     */
    String getMethodName();

    /**
     * Provider method arguments
     *
     * @return
     */
    Object[] getMethodArguments();

    /**
     * Retries count
     *
     * @return
     */
    int getRetries();
}

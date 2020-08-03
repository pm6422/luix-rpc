package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.Exchangable;

import java.util.concurrent.atomic.AtomicInteger;

public interface Requestable<T> extends Exchangable<T> {

    AtomicInteger RETRIES = new AtomicInteger(0);

    /**
     * Get client request ID
     *
     * @return
     */
    String getClientRequestId();

    /**
     * Set client request ID
     */
    T clientRequestId(String clientRequestId);

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
     * Set call retries count
     *
     * @param retries
     * @return
     */
    T retries(int retries);

    /**
     * Retries count
     *
     * @return
     */
    int getRetries();

    /**
     * Protocol
     *
     * @return
     */
    String getProtocol();

    /**
     * Protocol version
     *
     * @return
     */
    byte getProtocolVersion();
}

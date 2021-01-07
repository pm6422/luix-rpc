package org.infinity.rpc.core.exchange.transport;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.transport.exception.TransmissionException;
import org.infinity.rpc.core.url.Url;

import java.net.InetSocketAddress;

public interface Channel {

    /**
     * Send request
     *
     * @param request request object
     * @return response object
     * @throws TransmissionException if exception occurs
     */
    Responseable request(Requestable request) throws TransmissionException;

    /**
     * Open the channel
     *
     * @return true: channel opened, false: not yet opened
     */
    boolean open();

    /**
     * Close the channel
     */
    void close();

    /**
     * Close the channel with a timeout
     *
     * @param timeout timeout in @todo
     */
    void close(int timeout);

    /**
     * Get local socket address
     *
     * @return local address
     */
    InetSocketAddress getLocalAddress();

    /**
     * Get remote socket address
     *
     * @return remote address
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Check whether it is closed or not
     *
     * @return true: closed, false: not closed
     */
    boolean isClosed();

    /**
     * Check node availability status
     *
     * @return true: available, false: unavailable
     */
    boolean isActive();

    /**
     * Get the provider url
     *
     * @return url provider url
     */
    Url getUrl();
}
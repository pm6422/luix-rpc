package org.infinity.rpc.core.exchange.transmission;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.transmission.exception.TransmissionException;
import org.infinity.rpc.core.url.Url;

import java.net.InetSocketAddress;

public interface Channel {

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
     * @return true: opened, false: not opened
     */
    boolean isOpen();

    /**
     * Close the channel
     */
    void close();

    /**
     * Close the channel with a timeout
     * @param timeout timeout in @todo
     */
    void close(int timeout);

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
    boolean isAvailable();

    /**
     * Get the provider url
     *
     * @return url provider url
     */
    Url getUrl();
}
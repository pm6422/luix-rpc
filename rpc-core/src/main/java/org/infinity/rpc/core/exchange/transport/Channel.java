package org.infinity.rpc.core.exchange.transport;

import org.infinity.rpc.core.exception.TransportException;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.exchange.transport.constants.ChannelState;
import org.infinity.rpc.core.url.Url;

import java.net.InetSocketAddress;

public interface Channel {

    /**
     * Send request
     *
     * @param request request object
     * @return response object
     * @throws TransportException if exception occurs
     */
    Responseable request(Requestable request) throws TransportException;

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
     * Get state
     *
     * @return state
     */
    ChannelState getState();

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
    Url getProviderUrl();
}
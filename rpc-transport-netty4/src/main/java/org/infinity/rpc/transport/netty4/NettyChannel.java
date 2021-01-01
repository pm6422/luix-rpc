package org.infinity.rpc.transport.netty4;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.transmission.Channel;
import org.infinity.rpc.core.exchange.transmission.exception.TransmissionException;
import org.infinity.rpc.core.url.Url;

import java.net.InetSocketAddress;

public class NettyChannel implements Channel {
    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public Responseable request(Requestable request) throws TransmissionException {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Url getUrl() {
        return null;
    }
}

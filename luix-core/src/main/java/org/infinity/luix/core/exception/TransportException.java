package org.infinity.luix.core.exception;

import java.io.IOException;
import java.net.InetSocketAddress;

public class TransportException extends IOException {

    private static final long              serialVersionUID = 119354326473981551L;
    private final        InetSocketAddress localAddress;
    private final        InetSocketAddress remoteAddress;

    public TransportException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message) {
        super(message);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public TransportException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message, Throwable cause) {
        super(message, cause);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

}

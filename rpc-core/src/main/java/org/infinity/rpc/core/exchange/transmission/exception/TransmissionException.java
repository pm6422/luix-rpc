package org.infinity.rpc.core.exchange.transmission.exception;

import java.io.IOException;
import java.net.InetSocketAddress;

public class TransmissionException extends IOException {
    private static final long              serialVersionUID = 4978233711438730021L;
    private              InetSocketAddress localAddress;
    private              InetSocketAddress remoteAddress;

    public TransmissionException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message) {
        this(localAddress, remoteAddress, message, null);
    }

    public TransmissionException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message, Throwable cause) {
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
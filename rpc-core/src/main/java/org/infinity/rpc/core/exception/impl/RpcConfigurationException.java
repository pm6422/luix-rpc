package org.infinity.rpc.core.exception.impl;

public class RpcConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 5640903282132440106L;

    public RpcConfigurationException() {
        super();
    }

    public RpcConfigurationException(String message) {
        super(message);
    }

    public RpcConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

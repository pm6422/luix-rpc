package com.luixtech.luixrpc.core.exception.impl;

import com.luixtech.luixrpc.core.exception.RpcAbstractException;

/**
 * Configuration exception refers to an exception caused by incorrect configuration
 */
public class RpcConfigException extends RpcAbstractException {

    private static final long serialVersionUID = -157808925039174863L;

    public RpcConfigException() {
        super();
    }

    public RpcConfigException(String message) {
        super(message);
    }

    public RpcConfigException(Throwable cause) {
        super(cause);
    }

    public RpcConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}

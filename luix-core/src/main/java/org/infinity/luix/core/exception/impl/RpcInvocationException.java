package org.infinity.luix.core.exception.impl;

import org.infinity.luix.core.exception.RpcAbstractException;

/**
 * RPC invocation exception refers to the exception that occurs during the RPC call
 */
public class RpcInvocationException extends RpcAbstractException {

    private static final long serialVersionUID = -4974867821777714425L;

    public RpcInvocationException() {
        super();
    }

    public RpcInvocationException(String message) {
        super(message);
    }

    public RpcInvocationException(Throwable cause) {
        super(cause);
    }

    public RpcInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}

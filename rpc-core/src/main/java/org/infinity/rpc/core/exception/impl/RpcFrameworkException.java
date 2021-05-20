package org.infinity.rpc.core.exception.impl;

import org.infinity.rpc.core.exception.RpcAbstractException;

public class RpcFrameworkException extends RpcAbstractException {

    private static final long serialVersionUID = -3856975234463367637L;

    public RpcFrameworkException() {
        super();
    }

    public RpcFrameworkException(String message) {
        super(message);
    }

    public RpcFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcFrameworkException(String message, Throwable cause, RpcErrorMsg errorMsg) {
        super(message, cause, errorMsg);
    }

    public RpcFrameworkException(String message, RpcErrorMsg errorMsg) {
        super(message, errorMsg);
    }
}

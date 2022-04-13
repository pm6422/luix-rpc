package com.luixtech.rpc.core.exception.impl;

import com.luixtech.rpc.core.exception.RpcAbstractException;

public class RpcFrameworkException extends RpcAbstractException {

    private static final long serialVersionUID = -3046248028536194414L;

    public RpcFrameworkException() {
        super();
    }

    public RpcFrameworkException(String message) {
        super(message);
    }

    public RpcFrameworkException(Throwable cause) {
        super(cause);
    }

    public RpcFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }

}

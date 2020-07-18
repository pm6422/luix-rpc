package org.infinity.rpc.core.exception;

public class RpcInvocationException extends RuntimeException {

    private static final long serialVersionUID = 6043171729796528164L;

    public RpcInvocationException() {
        super();
    }

    public RpcInvocationException(String message) {
        super(message);
    }

    public RpcInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}

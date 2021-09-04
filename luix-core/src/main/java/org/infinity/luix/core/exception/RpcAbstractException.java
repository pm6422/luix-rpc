package org.infinity.luix.core.exception;

public abstract class RpcAbstractException extends RuntimeException {

    private static final long serialVersionUID = 2095011577273198213L;

    public RpcAbstractException() {
        super();
    }

    public RpcAbstractException(String message) {
        super(message);
    }

    public RpcAbstractException(Throwable cause) {
        super(cause);
    }

    public RpcAbstractException(String message, Throwable cause) {
        super(message, cause);
    }
}

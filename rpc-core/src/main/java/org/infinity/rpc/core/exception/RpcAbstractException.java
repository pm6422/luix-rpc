package org.infinity.rpc.core.exception;

import org.infinity.rpc.core.utils.RpcRequestIdHolder;

public abstract class RpcAbstractException extends RuntimeException {

    private static final long     serialVersionUID = 2095011577273198213L;
    protected            RpcError rpcError         = RpcErrorConstants.FRAMEWORK_DEFAULT_ERROR;
    protected            String   errorMsg         = null;

    public RpcAbstractException() {
        super();
    }

    public RpcAbstractException(RpcError rpcError) {
        super();
        this.rpcError = rpcError;
    }

    public RpcAbstractException(String message) {
        super(message);
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, RpcError rpcError) {
        super(message);
        this.rpcError = rpcError;
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, Throwable cause) {
        super(message, cause);
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, Throwable cause, RpcError rpcError) {
        super(message, cause);
        this.rpcError = rpcError;
        this.errorMsg = message;
    }

    public RpcAbstractException(Throwable cause) {
        super(cause);
    }

    public RpcAbstractException(Throwable cause, RpcError rpcError) {
        super(cause);
        this.rpcError = rpcError;
    }

    @Override
    public String getMessage() {
        String message = getOriginMessage();
        return "errorMsg: " + message + ", status: " + rpcError.getStatus() + ", errorCode: " + rpcError.getCode()
                + ", requestId: " + RpcRequestIdHolder.getRequestId();
    }

    public String getOriginMessage() {
        if (rpcError == null) {
            return super.getMessage();
        }

        String message;

        if (errorMsg != null && !"".equals(errorMsg)) {
            message = errorMsg;
        } else {
            message = rpcError.getMessage();
        }
        return message;
    }

    public int getStatus() {
        return rpcError != null ? rpcError.getStatus() : 0;
    }

    public int getErrorCode() {
        return rpcError != null ? rpcError.getCode() : 0;
    }

    public RpcError getRpcErrorMsg() {
        return rpcError;
    }
}

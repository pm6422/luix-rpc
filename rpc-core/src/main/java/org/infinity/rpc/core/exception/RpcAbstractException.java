package org.infinity.rpc.core.exception;

import org.infinity.rpc.core.exception.impl.RpcErrorMsg;
import org.infinity.rpc.core.utils.RpcRequestIdHolder;

public abstract class RpcAbstractException extends RuntimeException {

    private static final long        serialVersionUID = 2095011577273198213L;
    protected            RpcErrorMsg rpcErrorMsg      = RpcErrorConstants.FRAMEWORK_DEFAULT_ERROR;
    protected            String      errorMsg         = null;

    public RpcAbstractException() {
        super();
    }

    public RpcAbstractException(RpcErrorMsg rpcErrorMsg) {
        super();
        this.rpcErrorMsg = rpcErrorMsg;
    }

    public RpcAbstractException(String message) {
        super(message);
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, RpcErrorMsg rpcErrorMsg) {
        super(message);
        this.rpcErrorMsg = rpcErrorMsg;
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, Throwable cause) {
        super(message, cause);
        this.errorMsg = message;
    }

    public RpcAbstractException(String message, Throwable cause, RpcErrorMsg rpcErrorMsg) {
        super(message, cause);
        this.rpcErrorMsg = rpcErrorMsg;
        this.errorMsg = message;
    }

    public RpcAbstractException(Throwable cause) {
        super(cause);
    }

    public RpcAbstractException(Throwable cause, RpcErrorMsg rpcErrorMsg) {
        super(cause);
        this.rpcErrorMsg = rpcErrorMsg;
    }

    @Override
    public String getMessage() {
        String message = getOriginMessage();
        return "errorMsg: " + message + ", status: " + rpcErrorMsg.getStatus() + ", errorCode: " + rpcErrorMsg.getErrorCode()
                + ", requestId: " + RpcRequestIdHolder.getRequestId();
    }

    public String getOriginMessage() {
        if (rpcErrorMsg == null) {
            return super.getMessage();
        }

        String message;

        if (errorMsg != null && !"".equals(errorMsg)) {
            message = errorMsg;
        } else {
            message = rpcErrorMsg.getMessage();
        }
        return message;
    }

    public int getStatus() {
        return rpcErrorMsg != null ? rpcErrorMsg.getStatus() : 0;
    }

    public int getErrorCode() {
        return rpcErrorMsg != null ? rpcErrorMsg.getErrorCode() : 0;
    }

    public RpcErrorMsg getRpcErrorMsg() {
        return rpcErrorMsg;
    }
}

package org.infinity.rpc.core.exception;

public abstract class ExceptionUtils {

    public static boolean isBizException(Throwable t) {
        return t instanceof RpcBizException;
    }

    public static boolean isRpcException(Throwable t) {
        return t instanceof RpcAbstractException;
    }
}

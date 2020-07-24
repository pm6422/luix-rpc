package org.infinity.rpc.core.utils;

import org.infinity.rpc.core.exception.RpcBizException;

public abstract class ExceptionUtils {

    public static boolean isBizException(Throwable t) {
        return t instanceof RpcBizException;
    }
}

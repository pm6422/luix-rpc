package org.infinity.rpc.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.exception.impl.RpcBizException;

@Slf4j
public abstract class ExceptionUtils {

    public static final StackTraceElement[] MOCK_STACK = new StackTraceElement[]{
            new StackTraceElement("remoteClass", "remoteMethod", "remoteFile", 1)};

    public static boolean isRpcException(Throwable t) {
        return t instanceof RpcAbstractException;
    }

    public static boolean isBizException(Throwable t) {
        return t instanceof RpcBizException;
    }

    /**
     * Replace the stack trace of the exception with the mock one
     *
     * @param cause exception
     */
    public static void setMockStackTrace(Throwable cause) {
        Validate.notNull(cause, "Cause must NOT be null!");
        try {
            cause.setStackTrace(MOCK_STACK);
        } catch (Exception ex) {
            log.warn("Failed to replace exception stack!", ex);
        }
    }
}

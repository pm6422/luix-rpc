package org.infinity.rpc.core.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ExceptionUtils {

    public static final StackTraceElement[] REMOTE_MOCK_STACK = new StackTraceElement[]{new StackTraceElement("remoteClass",
            "remoteMethod", "remoteFile", 1)};

    public static boolean isBizException(Throwable t) {
        return t instanceof RpcBizException;
    }

    public static boolean isRpcException(Throwable t) {
        return t instanceof RpcAbstractException;
    }

    /**
     * 覆盖给定exception的stack信息，server端产生业务异常时调用此类屏蔽掉server端的异常栈。
     *
     * @param e exception
     */
    public static void setMockStackTrace(Throwable e) {
        if (e != null) {
            try {
                e.setStackTrace(REMOTE_MOCK_STACK);
            } catch (Exception e1) {
                log.warn("replace remote exception stack fail!" + e1.getMessage());
            }
        }
    }
}

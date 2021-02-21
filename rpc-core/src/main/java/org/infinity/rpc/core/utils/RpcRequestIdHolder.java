package org.infinity.rpc.core.utils;

public abstract class RpcRequestIdHolder {
    private static final ThreadLocal<Long> HOLDER = ThreadLocal.withInitial(() -> 0L);

    public static Long getRequestId() {
        return HOLDER.get();
    }

    public static void setRequestId(Long requestId) {
        HOLDER.set(requestId);
    }

    public static void destroy() {
        HOLDER.remove();
    }
}

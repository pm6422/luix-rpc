package org.infinity.rpc.democlient.utils;

public abstract class RequesterIdHolder {
    private static final ThreadLocal<String> HOLDER = ThreadLocal.withInitial(() -> "");

    public static String getRequesterId() {
        return HOLDER.get();
    }

    public static void setRequesterId(String requesterId) {
        HOLDER.set(requesterId);
    }

    public static void destroy() {
        HOLDER.remove();
    }
}

package org.infinity.rpc.utilities.id;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class RequestIdGenerator {
    private RequestIdGenerator() {
    }

    // 毫秒内固定起始值开始
    private static final Sequence SEQUENCE = new Sequence(1L, false, false);

    /**
     * Thread-safe
     * Can guarantee unique on multi-threads environment
     *
     * @return
     */
    public static long getRequestId() {
        return SEQUENCE.nextId();
    }
}

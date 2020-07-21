package org.infinity.rpc.utilities.id;

public class RequestIdGenerator {

    // 毫秒内固定起始值开始
    private static final Sequence SEQUENCE = new Sequence(1L, false, false);

    public static long getRequestId() {
        return SEQUENCE.nextId();
    }
}

package org.infinity.rpc.utilities.id;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class IdGenerator {
    private IdGenerator() {
    }

    // 毫秒内固定起始值开始
    private static final SnowFlakeSequence SNOW_FLAKE_SEQUENCE = new SnowFlakeSequence(1L, false, false);

    /**
     * Thread-safe
     * Can guarantee unique on multi-threads environment
     *
     * @return
     */
    public static long generateSnowFlakeId() {
        return SNOW_FLAKE_SEQUENCE.nextId();
    }

    /**
     * Thread-safe
     * Can guarantee unique on multi-threads environment
     *
     * @return
     */
    public static long generateTimestampId() {
        return TimestampSequence.nextId();
    }
}

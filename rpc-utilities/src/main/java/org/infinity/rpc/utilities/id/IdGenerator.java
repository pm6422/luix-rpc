package org.infinity.rpc.utilities.id;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class IdGenerator {
    private static final ShortId     SHORT_SEQUENCE      = new ShortId();
    // 毫秒内固定起始值开始
    private static final SnowFlakeId SNOW_FLAKE_SEQUENCE = new SnowFlakeId(1L, false, false);

    private IdGenerator() {
    }

    /**
     * Thread-safe
     * Can guarantee unique on multi-threads environment
     *
     * @return 18 bits length，like：317297928250941551
     */
    public static long generateSnowFlakeId() {
        return SNOW_FLAKE_SEQUENCE.nextId();
    }

    /**
     * Thread-safe
     * Can guarantee unique on multi-threads environment
     *
     * @return 19 bits length，like：1672888135850179037
     */
    public static long generateTimestampId() {
        return TimestampId.nextId();
    }

    /**
     * Thread-safe
     * Can guarantee unique on multi-threads environment
     *
     * @return 12 bits length，like：306554419571
     */
    public static long generateShortId() {
        return SHORT_SEQUENCE.nextId();
    }
}

package org.infinity.rpc.utilities.id;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@NotThreadSafe
public class RequestIdGenerator {
    private static Object          lock       = new Object();
    private        ExecutorService threadPool = Executors.newSingleThreadExecutor();

    private RequestIdGenerator() {
    }

    // 毫秒内固定起始值开始
    private static final Sequence SEQUENCE = new Sequence(1L, false, false);

    /**
     * Not Thread-safe
     * Can Not guarantee unique on multi-threads env
     *
     * @return
     */
    public static long getRequestId() {
        return SEQUENCE.nextId();
    }
}

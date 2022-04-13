package com.luixtech.luixrpc.metrics;

import com.luixtech.uidgenerator.core.id.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class MetricsUtilsTests {

    @Test
    public void test() throws InterruptedException {
        MetricsUtils.trackCall("rpc-demo-client", IdGenerator.generateTimestampId(),
                7, 2, 200, ResponseType.NORMAL);
        MetricsUtils.trackCall("rpc-demo-client", IdGenerator.generateTimestampId(),
                250, 210, 200, ResponseType.NORMAL);
        MetricsUtils.trackCall("rpc-demo-server", IdGenerator.generateTimestampId(),
                80, 70, 200, ResponseType.BIZ_EXCEPTION);

        // It must sleep for a while
        Thread.sleep(1000L);
    }
}

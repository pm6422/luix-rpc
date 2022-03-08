package org.infinity.luix.metrics.statistic;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.metrics.statistic.access.ResponseType;
import org.junit.jupiter.api.Test;

@Slf4j
public class MetricsUtilsTests {

    @Test
    public void test() throws InterruptedException {
        MetricsUtils.trackCall("rpc-demo-client",
                7, 2, 200, ResponseType.NORMAL);
        MetricsUtils.trackCall("rpc-demo-client",
                250, 210, 200, ResponseType.NORMAL);

        MetricsUtils.trackCall("rpc-demo-server",
                80, 70, 200, ResponseType.BIZ_EXCEPTION);

        // It must sleep for a while
        Thread.sleep(1000L);
    }
}

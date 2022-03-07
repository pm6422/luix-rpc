package org.infinity.luix.metrics.statistic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.metrics.statistic.access.CallMetric;
import org.infinity.luix.metrics.statistic.access.ResponseType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentMap;

@Slf4j
public class MetricsUtilsTests {

    @Test
    public void test() throws InterruptedException, JsonProcessingException {
        MetricsUtils.trackCall("rpc-demo-client",
                System.currentTimeMillis(), 7, 2, 200, ResponseType.NORMAL);
        MetricsUtils.trackCall("rpc-demo-client",
                System.currentTimeMillis(), 250, 210, 200, ResponseType.NORMAL);

        MetricsUtils.trackCall("rpc-demo-server",
                System.currentTimeMillis(), 80, 70, 200, ResponseType.BIZ_EXCEPTION);

        // It must sleep for a while
        Thread.sleep(1000L);
        ConcurrentMap<String, CallMetric> totalAccessStatistic = MetricsUtils.getAllCallMetrics();
        log.info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(totalAccessStatistic));
    }
}

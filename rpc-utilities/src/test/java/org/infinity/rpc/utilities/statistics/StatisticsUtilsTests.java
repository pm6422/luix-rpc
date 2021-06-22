package org.infinity.rpc.utilities.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.utilities.statistics.access.AccessStatisticResult;
import org.infinity.rpc.utilities.statistics.access.StatisticType;
import org.junit.Test;

import java.util.concurrent.ConcurrentMap;

@Slf4j
public class StatisticsUtilsTests {

    @Test
    public void test() throws InterruptedException, JsonProcessingException {
        StatisticsUtils.logAccess("requestProcess", "rpc-demo-client", "rpc-demo-client",
                System.currentTimeMillis(), 7, 2, 200, StatisticType.NORMAL);
        StatisticsUtils.logAccess("requestProcess", "rpc-demo-client", "rpc-demo-client",
                System.currentTimeMillis(), 250, 210, 200, StatisticType.NORMAL);

        StatisticsUtils.logAccess("responseProcess", "rpc-demo-server", "rpc-demo-server",
                System.currentTimeMillis(), 80, 70, 200, StatisticType.BIZ_EXCEPTION);

        // It must sleep for a while
        Thread.sleep(1000L);
        ConcurrentMap<String, AccessStatisticResult> totalAccessStatistic = StatisticsUtils.getTotalAccessStatistic();
        log.info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(totalAccessStatistic));
    }
}

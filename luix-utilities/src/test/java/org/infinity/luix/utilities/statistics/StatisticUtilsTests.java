package org.infinity.luix.utilities.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.utilities.statistics.access.AccessStatisticResult;
import org.infinity.luix.utilities.statistics.access.StatisticType;
import org.junit.Test;

import java.util.concurrent.ConcurrentMap;

@Slf4j
public class StatisticUtilsTests {

    @Test
    public void test() throws InterruptedException, JsonProcessingException {
        StatisticUtils.logAccess("requestProcess", "rpc-demo-client", "rpc-demo-client",
                System.currentTimeMillis(), 7, 2, 200, StatisticType.NORMAL);
        StatisticUtils.logAccess("requestProcess", "rpc-demo-client", "rpc-demo-client",
                System.currentTimeMillis(), 250, 210, 200, StatisticType.NORMAL);

        StatisticUtils.logAccess("responseProcess", "rpc-demo-server", "rpc-demo-server",
                System.currentTimeMillis(), 80, 70, 200, StatisticType.BIZ_EXCEPTION);

        // It must sleep for a while
        Thread.sleep(1000L);
        ConcurrentMap<String, AccessStatisticResult> totalAccessStatistic = StatisticUtils.getTotalAccessStatistic();
        log.info(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(totalAccessStatistic));
    }
}

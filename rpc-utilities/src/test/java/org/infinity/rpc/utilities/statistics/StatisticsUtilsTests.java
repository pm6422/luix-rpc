package org.infinity.rpc.utilities.statistics;

import org.infinity.rpc.utilities.statistics.access.StatisticType;
import org.junit.Test;

public class StatisticsUtilsTests {

    @Test
    public void test() throws InterruptedException {
        long now1 = System.currentTimeMillis();
        StatisticsUtils.logAccess("rpcRequestProcess", "rpc-demo-client", "rpc-demo-client",
                now1, now1 - 100, now1 - 200, 50, StatisticType.NORMAL);

        Thread.sleep(100L);
        long now2 = System.currentTimeMillis();
        StatisticsUtils.logAccess("rpcRequestProcess", "rpc-demo-client", "rpc-demo-client",
                now2, now2 - 100, now2 - 200, 50, StatisticType.NORMAL);

        Thread.sleep(100L);
        long now3 = System.currentTimeMillis();
        StatisticsUtils.logAccess("rpcRequestProcess", "rpc-demo-server", "rpc-demo-server",
                now3, now3 - 100, now3 - 200, 50, StatisticType.BIZ_EXCEPTION);
    }
}

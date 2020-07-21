package org.infinity.rpc.utilities.id;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 重复性测试
 */
@Slf4j
public class RepetitionTest {
    /**
     * single thread test
     *
     * @throws InterruptedException
     */
    @Test
    public void singleThreadTest() throws InterruptedException {
        Set<Long> set = new HashSet<>();
        int maxTimes = 10000 * 10;
        Sequence sequence = new Sequence(0);
        // Single thread
        ExecutorService threadPool = Executors.newFixedThreadPool(1);

        IntStream.range(0, maxTimes).forEach(i -> {
            threadPool.execute(() -> {
                log.debug("Active thread count: {}", Thread.activeCount());
                set.add(sequence.nextId());
            });
        });

        threadPool.shutdown();
        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
            Assert.assertEquals(maxTimes, set.size());
        }
    }

    /**
     * multi-thread test
     *
     * @throws InterruptedException
     */
//    @Test
//    public void multiThreadThreadTest() throws InterruptedException {
//        Set<Long> set = new HashSet<>();
//        int maxTimes = 10000 * 10;
//        Sequence sequence = new Sequence(0);
//        // Two threads
//        ExecutorService threadPool = Executors.newFixedThreadPool(2);
//
//        IntStream.range(0, maxTimes).forEach(i -> {
//            threadPool.execute(() -> {
//                log.debug("Active thread count: {}", Thread.activeCount());
//                set.add(sequence.nextId());
//            });
//        });
//
//        threadPool.shutdown();
//        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
//            Assert.assertEquals(maxTimes, set.size());
//        }
//    }
}